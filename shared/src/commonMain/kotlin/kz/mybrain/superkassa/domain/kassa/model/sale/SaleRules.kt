package kz.mybrain.superkassa.domain.kassa.model.sale

import kz.mybrain.superkassa.domain.kassa.model.payment.CASH_PAYMENT
import kz.mybrain.superkassa.domain.kassa.model.payment.SplitIssue
import kz.mybrain.superkassa.domain.kassa.model.payment.UNSUPPORTED_PAYMENTS

/**
 * Всё, от чего зависит, можно ли пробить чек.
 *
 * Собрано в один снимок намеренно: правила проверяются без экрана,
 * а экран только показывает найденную причину. Суммы — в тиынах.
 */
data class SaleState(
    val hasKkm: Boolean = true,
    val kkmBlocked: Boolean = false,
    val hasPin: Boolean = true,
    val shiftOpen: Boolean = true,
    val positions: Int = 1,
    val hasItemDiscount: Boolean = false,
    /** Стоит ли в чеке строка, стоимость которой вышла нулевой. */
    val hasZeroLine: Boolean = false,
    /** Скидка на весь чек, как её набрал кассир: суммой или процентом. */
    val discount: Adjustment = Adjustment(),
    /** Наценка на чек: проверяется теми же правилами, что и скидка. */
    val markup: Adjustment = Adjustment(),
    /** Сумма позиций: от неё считается процент и ею же ограничена скидка. */
    val itemsSum: Long = 0L,
    val total: Long = ONE_TENGE,
    val paymentCodes: List<String> = listOf(CASH_PAYMENT),
    /** Чем разбиение оплаты не годится, если оплат несколько. */
    val splitIssue: SplitIssue? = null,
    /**
     * Виды оплаты, которые касса сейчас не принимает.
     *
     * Приходят из справочника кассы: там у каждого вида есть признак
     * допустимости. Пока справочник не прочитан, берётся последнее
     * известное состояние протокола.
     */
    val unsupportedPayments: Set<String> = UNSUPPORTED_PAYMENTS,
    val taken: Long? = null,
    /** Наличная часть чека: с неё берётся сдача. `null` — весь чек наличными. */
    val cashSum: Long? = null,
    val customerBin: String = "",
    /** Контакт покупателя набран, но не разобран: чек ушёл бы в никуда. */
    val contactMalformed: Boolean = false,
    /**
     * Отраслевой реквизит, которого не хватает, или `null`.
     *
     * Вид отрасли — настройка кассы, и у кассы в торговле это поле пусто
     * всегда: заполнять ей нечего.
     */
    val missingDomainField: DomainField? = null
)

/**
 * Причина, по которой чек пробить нельзя.
 *
 * Словами кассира её называют надписи области: он должен понять, что
 * исправить, не зная ни кода отказа, ни номера версии протокола.
 */
enum class SaleBlock {
    NoKkm,
    NoPin,
    KkmBlocked,
    ShiftClosed,
    EmptyBasket,
    ZeroLine,
    DomainFields,
    PaymentUnsupported,
    PaymentSplitEmpty,
    PaymentSplitExcess,
    DiscountScopes,
    ChangeNotANumber,
    DiscountNegative,
    PercentOverHundred,
    DiscountOverItems,
    TotalNotPositive,
    CustomerBin,
    CustomerContact,
    TakenTooSmall
}

/**
 * Первая причина, мешающая пробить чек, или `null`, если помех нет.
 *
 * Порядок от общего к частному: пока касса не выбрана, разбираться
 * в скидках бессмысленно, и показывать кассиру нужно именно то, с чего
 * начинать. Нулевая строка названа прежде итога: итог с соседними
 * позициями положителен, и общая причина о ней не сказала бы.
 * Отраслевой реквизит — раньше оплаты: касса такой чек пропускает,
 * а БФД отвергает, когда исправлять уже нечего.
 */
fun blockOf(state: SaleState): SaleBlock? =
    firstBroken(BEFORE_CHANGES, state) ?: splitBlockOf(state.splitIssue) ?: changeBlockOf(state)
        ?: firstBroken(AFTER_CHANGES, state)

/** Причина — и чем она определяется по снимку чека. */
private typealias SaleCheck = Pair<SaleBlock, (SaleState) -> Boolean>

private fun firstBroken(checks: List<SaleCheck>, state: SaleState): SaleBlock? =
    checks.firstOrNull { (_, broken) -> broken(state) }?.first

/** Касса, смена, состав чека, реквизиты и виды оплаты — до денег. */
private val BEFORE_CHANGES: List<SaleCheck> = listOf(
    SaleBlock.NoKkm to { !it.hasKkm },
    SaleBlock.NoPin to { !it.hasPin },
    SaleBlock.KkmBlocked to { it.kkmBlocked },
    SaleBlock.ShiftClosed to { !it.shiftOpen },
    SaleBlock.EmptyBasket to { it.positions == 0 },
    SaleBlock.ZeroLine to { it.hasZeroLine },
    SaleBlock.DomainFields to { it.missingDomainField != null },
    SaleBlock.PaymentUnsupported to { state -> state.paymentCodes.any { it in state.unsupportedPayments } }
)

/** Итог, покупатель и принятые деньги — после скидок. */
private val AFTER_CHANGES: List<SaleCheck> = listOf(
    SaleBlock.TotalNotPositive to { it.total <= 0L },
    SaleBlock.CustomerBin to { !binAccepted(it.customerBin) },
    SaleBlock.CustomerContact to { it.contactMalformed },
    SaleBlock.TakenTooSmall to ::takenTooSmall
)

private fun splitBlockOf(issue: SplitIssue?): SaleBlock? = when (issue) {
    SplitIssue.Empty -> SaleBlock.PaymentSplitEmpty
    SplitIssue.Excess -> SaleBlock.PaymentSplitExcess
    null -> null
}

/**
 * Сдача покупателю, в тиынах.
 *
 * Считается от наличной части чека, а не от итога: при оплате картой
 * и наличными вместе сдача с итога ушла бы в ноль и покупатель
 * недополучил бы свои деньги.
 *
 * Отрицательной сдачи не бывает: «сдача −200 ₸» кассиру ничего не
 * объясняет, а недостачу называет отдельная причина.
 */
fun changeOf(taken: Long?, cashSum: Long): Long? = taken?.takeIf { it >= cashSum }?.minus(cashSum)

/**
 * ИИН/БИН покупателя необязателен, но если введён — ровно двенадцать цифр.
 *
 * Касса длину не проверяет и такой чек примет: отвергнет его уже БФД,
 * когда исправлять будет нечего.
 */
fun binAccepted(bin: String): Boolean =
    bin.isEmpty() || (bin.length == BIN_LENGTH && bin.all(Char::isDigit))

private fun takenTooSmall(state: SaleState): Boolean {
    val cash = state.cashSum ?: state.total
    if (cash <= 0L) return false
    val taken = state.taken ?: return false
    return taken < cash
}

/** Ровно столько цифр в ИИН и в БИН. */
const val BIN_LENGTH: Int = 12

/** Итог снимка по умолчанию: положительный, чтобы правило итога молчало. */
private const val ONE_TENGE: Long = 100L
