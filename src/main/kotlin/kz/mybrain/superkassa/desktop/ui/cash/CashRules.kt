package kz.mybrain.superkassa.desktop.ui.cash

import kz.mybrain.superkassa.desktop.ui.components.Money
import java.math.BigDecimal

/** Что кассир делает с денежным ящиком. */
enum class CashMove { Deposit, Withdraw }

/**
 * Почему сумма не будет проведена.
 *
 * Причина отделена от надписи намеренно: правило проверяется тестом,
 * а текст выбирается языком кассира.
 */
enum class CashRefusal { NotANumber, NotPositive, TooLarge, NotEnough, ShiftClosed }

/** Приговор введённой сумме. */
sealed interface CashDecision {
    /** Кассир ещё ничего не ввёл: это не ошибка, а пустое поле. */
    data object Empty : CashDecision

    data class Ready(val amount: BigDecimal) : CashDecision

    data class Refused(val reason: CashRefusal) : CashDecision
}

/**
 * Попытка провести сумму: что, сколько и под каким ключом.
 *
 * Ключ живёт вместе с попыткой, а не создаётся на каждое нажатие: узел
 * отличает повтор от новой операции только по нему, и вторая попытка той
 * же суммы обязана прийти с прежним ключом.
 */
data class CashAttempt(val move: CashMove, val amount: BigDecimal, val key: String)

/**
 * Правила движения наличных до обращения к узлу.
 *
 * Узел проверяет то же самое и отвечает отказом, но кассир стоит перед
 * покупателем: сказать «сумма должна быть больше нуля» на месте дешевле,
 * чем сходить за этим на сервер.
 *
 * Последнее слово всё равно за узлом: остаток он считает по документам
 * смены, и если его расчёт строже — откажет он.
 */
object CashRules {

    /**
     * Предел одной операции.
     *
     * Ограничение не протокольное, а от опечатки: лишний ноль в сумме
     * изъятия превращает тысячу в десять тысяч, и заметить это после
     * фискализации уже нельзя.
     */
    val maxAmount: BigDecimal = BigDecimal("99999999.99")

    fun check(text: String, move: CashMove, drawerTiyn: Long?, shiftOpen: Boolean): CashDecision {
        if (text.isBlank()) return CashDecision.Empty
        val amount = Money.parse(text) ?: return CashDecision.Refused(CashRefusal.NotANumber)
        if (amount <= BigDecimal.ZERO) return CashDecision.Refused(CashRefusal.NotPositive)
        if (amount > maxAmount) return CashDecision.Refused(CashRefusal.TooLarge)
        if (!shiftOpen) return CashDecision.Refused(CashRefusal.ShiftClosed)
        if (move == CashMove.Withdraw && drawerTiyn != null && tiynOf(amount) > drawerTiyn) {
            return CashDecision.Refused(CashRefusal.NotEnough)
        }
        return CashDecision.Ready(amount)
    }

    /**
     * Сколько останется в ящике после операции.
     *
     * Показывается до проведения: изъятие «под ноль» и изъятие лишней
     * тысячи выглядят в поле ввода одинаково, а в ящике — по-разному.
     */
    fun after(drawerTiyn: Long?, amount: BigDecimal, move: CashMove): Long? {
        val current = drawerTiyn ?: return null
        val delta = tiynOf(amount)
        return if (move == CashMove.Deposit) current + delta else current - delta
    }

    /**
     * Ключ для очередной попытки.
     *
     * Та же сумма тем же движением — та же операция: кассир нажал ещё раз,
     * не дождавшись ответа, и провести деньги дважды нельзя. Изменил сумму —
     * это уже другая операция, и ключ нужен новый.
     */
    fun attemptFor(
        previous: CashAttempt?,
        move: CashMove,
        amount: BigDecimal,
        freshKey: () -> String
    ): CashAttempt {
        val same = previous != null && previous.move == move && previous.amount.compareTo(amount) == 0
        return if (same && previous != null) previous else CashAttempt(move, amount, freshKey())
    }

    /** Сумма в тиынах: остаток ящика узел хранит в них. */
    fun tiynOf(amount: BigDecimal): Long = amount.movePointRight(DECIMALS).toLong()

    private const val DECIMALS = 2
}
