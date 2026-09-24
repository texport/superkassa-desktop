package kz.mybrain.superkassa.domain.kassa.model.cash

import kz.mybrain.superkassa.domain.kassa.model.Tenge
import kz.mybrain.superkassa.domain.kassa.model.attemptKey

/** Что кассир делает с денежным ящиком. */
enum class CashMove { Deposit, Withdraw }

/**
 * Почему сумма не будет проведена.
 *
 * Причина отделена от надписи намеренно: правило проверяется тестом,
 * а текст выбирается языком кассира.
 */
enum class CashRefusal { NotANumber, NotPositive, TooLarge, NotEnough, ShiftClosed, KkmBlocked }

/** Приговор введённой сумме. */
sealed interface CashDecision {
    /** Кассир ещё ничего не ввёл: это не ошибка, а пустое поле. */
    data object Empty : CashDecision

    /** Сумма принята; [amount] — в тиынах. */
    data class Ready(val amount: Long) : CashDecision

    data class Refused(val reason: CashRefusal) : CashDecision
}

/**
 * Попытка провести сумму: что, сколько и под каким ключом.
 *
 * Ключ живёт вместе с попыткой, а не создаётся на каждое нажатие: узел
 * отличает повтор от новой операции только по нему, и вторая попытка той
 * же суммы обязана прийти с прежним ключом.
 *
 * @property amount сумма в тиынах.
 */
data class CashAttempt(val move: CashMove, val amount: Long, val key: String)

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
    const val MAX_AMOUNT: Long = 9_999_999_999L

    /**
     * @param kkmBlocked заблокирована ли касса — в том числе снята с учёта.
     * Узел движения наличных такой кассе не проводит, а смена у неё может
     * оставаться открытой: без этой проверки открытая смена снятой с учёта
     * кассы снова делала кнопки доступными.
     */
    fun check(
        text: String,
        move: CashMove,
        drawerTiyn: Long?,
        shiftOpen: Boolean,
        kkmBlocked: Boolean = false
    ): CashDecision {
        if (text.isBlank()) return CashDecision.Empty
        val amount = Tenge.parse(text) ?: return CashDecision.Refused(CashRefusal.NotANumber)
        if (amount <= 0L) return CashDecision.Refused(CashRefusal.NotPositive)
        if (amount > MAX_AMOUNT) return CashDecision.Refused(CashRefusal.TooLarge)
        if (kkmBlocked) return CashDecision.Refused(CashRefusal.KkmBlocked)
        if (!shiftOpen) return CashDecision.Refused(CashRefusal.ShiftClosed)
        if (move == CashMove.Withdraw && drawerTiyn != null && amount > drawerTiyn) {
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
    fun after(drawerTiyn: Long?, amount: Long, move: CashMove): Long? {
        val current = drawerTiyn ?: return null
        return if (move == CashMove.Deposit) current + amount else current - amount
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
        amount: Long,
        freshKey: () -> String = { attemptKey(KEY_KIND) }
    ): CashAttempt {
        val same = previous?.takeIf { it.move == move && it.amount == amount }
        return same ?: CashAttempt(move, amount, freshKey())
    }

    private const val KEY_KIND = "cash"
}

/**
 * Что мешает провести деньги — одно на оба движения.
 *
 * @property mistake ошибка в набранном — тогда поле красное. Состояние
 *   кассы поле красным не красит: введено верно.
 */
data class CashHoldup(val reason: CashRefusal, val mistake: Boolean)

/**
 * Помеха под полем суммы; `null` — помех нет, и под полем подсказка ввода.
 *
 * Строка под полем одна: три предупреждения столбиком читаются как три
 * разные беды. Закрытая смена и нехватка денег в ящике поле красным не
 * красят — введено верно, мешает состояние кассы, а не набранные цифры.
 *
 * Состояние кассы названо раньше набранного и независимо от него: при
 * пустом поле правило отвечает «ещё ничего не введено», и экран закрытой
 * смены выходил неотличимым от обычного — две погашенные кнопки и ни
 * слова о том, почему они погасли. Блокировка — прежде смены: снятой
 * с учёта кассе касса движений наличных не проводит.
 */
fun cashHoldup(deposit: CashDecision, withdraw: CashDecision, shiftOpen: Boolean, kkmBlocked: Boolean): CashHoldup? {
    val refused = (deposit as? CashDecision.Refused)?.reason
    val shortage = CashRefusal.NotEnough.takeIf { (withdraw as? CashDecision.Refused)?.reason == it }
    val reason = when {
        kkmBlocked -> CashRefusal.KkmBlocked
        !shiftOpen -> CashRefusal.ShiftClosed
        else -> refused ?: shortage
    }
    return reason?.let { CashHoldup(it, mistake = it == refused && it in INPUT_MISTAKES) }
}

/** Причины, в которых виноваты набранные цифры, а не касса. */
private val INPUT_MISTAKES =
    setOf(CashRefusal.NotANumber, CashRefusal.NotPositive, CashRefusal.TooLarge, CashRefusal.NotEnough)
