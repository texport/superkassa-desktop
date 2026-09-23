package kz.mybrain.superkassa.desktop.ui.sale

import kz.mybrain.superkassa.desktop.ui.payment.CASH_PAYMENT
import kz.mybrain.superkassa.desktop.ui.payment.SplitIssue
import kz.mybrain.superkassa.desktop.ui.payment.UNSUPPORTED_PAYMENTS
import kz.mybrain.superkassa.desktop.ui.strings.PaymentTexts
import kz.mybrain.superkassa.desktop.ui.strings.SaleTexts
import java.math.BigDecimal

/**
 * Всё, от чего зависит, можно ли пробить чек.
 *
 * Собрано в один снимок намеренно: правила проверяются вне Compose и
 * тестируются без запуска экрана, а экран только показывает найденную
 * причину.
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
    val itemsSum: BigDecimal = BigDecimal.ZERO,
    val total: BigDecimal = BigDecimal.ONE,
    val paymentCodes: List<String> = listOf(CASH_PAYMENT),
    /** Чем разбиение оплаты не годится, если оплат несколько. */
    val splitIssue: SplitIssue? = null,
    /**
     * Виды оплаты, которые узел сейчас не принимает.
     *
     * Приходят из справочника узла: там у каждого вида есть признак
     * допустимости. Пока справочник не прочитан, берётся последнее
     * известное состояние протокола.
     */
    val unsupportedPayments: Set<String> = UNSUPPORTED_PAYMENTS,
    val taken: BigDecimal? = null,
    /** Наличная часть чека: с неё берётся сдача. `null` — весь чек наличными. */
    val cashSum: BigDecimal? = null,
    val customerBin: String = "",
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
 * Каждая причина названа словами кассира: он должен понять, что исправить,
 * не зная ни кода отказа, ни номера версии протокола.
 */
enum class SaleBlock(private val text: (SaleTexts, PaymentTexts) -> String) {
    NoKkm({ sale, _ -> sale.blockNoKkm }),
    NoPin({ sale, _ -> sale.blockNoPin }),
    KkmBlocked({ sale, _ -> sale.blockKkmBlocked }),
    ShiftClosed({ sale, _ -> sale.blockShiftClosed }),
    EmptyBasket({ sale, _ -> sale.blockEmptyBasket }),
    ZeroLine({ sale, _ -> sale.blockZeroLine }),
    DomainFields({ sale, _ -> sale.fillIn }),
    PaymentUnsupported({ sale, _ -> sale.blockPaymentUnsupported }),
    PaymentSplitEmpty({ _, payment -> payment.splitEmpty }),
    PaymentSplitExcess({ _, payment -> payment.splitExcess }),
    DiscountScopes({ sale, _ -> sale.blockDiscountScopes }),
    DiscountNegative({ sale, _ -> sale.blockDiscountNegative }),
    PercentOverHundred({ sale, _ -> sale.blockPercentRange }),
    DiscountOverItems({ sale, _ -> sale.blockDiscountOverItems }),
    TotalNotPositive({ sale, _ -> sale.blockTotalNotPositive }),
    CustomerBin({ sale, _ -> sale.blockBin }),
    TakenTooSmall({ sale, _ -> sale.blockTakenTooSmall });

    /**
     * Причина словами кассира.
     *
     * Незаполненный отраслевой реквизит называется поимённо: «заполните
     * реквизиты» не говорит кассиру, какое поле пустует, а полей у такси
     * два.
     */
    fun reason(texts: SaleTexts, payment: PaymentTexts, field: DomainField? = null): String =
        if (this == DomainFields && field != null) field.reason(texts) else text(texts, payment)
}

/**
 * Первая причина, мешающая пробить чек, или `null`, если помех нет.
 *
 * Порядок от общего к частному: пока касса не выбрана, разбираться
 * в скидках бессмысленно, и показывать кассиру нужно именно то, с чего
 * начинать.
 */
@Suppress("ReturnCount")
fun blockOf(state: SaleState): SaleBlock? {
    if (!state.hasKkm) return SaleBlock.NoKkm
    if (!state.hasPin) return SaleBlock.NoPin
    if (state.kkmBlocked) return SaleBlock.KkmBlocked
    if (!state.shiftOpen) return SaleBlock.ShiftClosed
    if (state.positions == 0) return SaleBlock.EmptyBasket
    // Нулевая строка названа прежде итога: итог с соседними позициями
    // положителен, и общая причина «итог должен быть больше нуля»
    // о нулевой строке кассиру не сказала бы.
    if (state.hasZeroLine) return SaleBlock.ZeroLine
    // Отраслевой реквизит назван раньше оплаты: узел такой чек пропускает,
    // а БФД отвергает — когда исправлять уже нечего.
    if (state.missingDomainField != null) return SaleBlock.DomainFields
    if (state.paymentCodes.any { it in state.unsupportedPayments }) return SaleBlock.PaymentUnsupported
    when (state.splitIssue) {
        SplitIssue.Empty -> return SaleBlock.PaymentSplitEmpty
        SplitIssue.Excess -> return SaleBlock.PaymentSplitExcess
        null -> Unit
    }
    changeBlockOf(state)?.let { return it }
    if (state.total <= BigDecimal.ZERO) return SaleBlock.TotalNotPositive
    if (!binAccepted(state.customerBin)) return SaleBlock.CustomerBin
    if (takenTooSmall(state)) return SaleBlock.TakenTooSmall
    return null
}

/**
 * Сдача покупателю.
 *
 * Считается от наличной части чека, а не от итога: при оплате картой
 * и наличными вместе сдача с итога ушла бы в ноль и покупатель
 * недополучил бы свои деньги.
 *
 * Отрицательной сдачи не бывает: «сдача −200 ₸» кассиру ничего не
 * объясняет, а недостачу называет отдельная причина.
 */
fun changeOf(taken: BigDecimal?, cashSum: BigDecimal): BigDecimal? =
    taken?.takeIf { it >= cashSum }?.subtract(cashSum)

/**
 * ИИН/БИН покупателя необязателен, но если введён — ровно двенадцать цифр.
 *
 * Узел длину не проверяет и такой чек примет: отвергнет его уже БФД,
 * когда исправлять будет нечего.
 */
fun binAccepted(bin: String): Boolean =
    bin.isEmpty() || (bin.length == BIN_LENGTH && bin.all(Char::isDigit))

/**
 * Чем негодна скидка или наценка на весь чек.
 *
 * Знак, граница процента и размер скидки проверяются вместе: набрано одно
 * число, и кассиру называется одна причина, а не та, что ближе к началу
 * списка. Порядок внутри — от смысла к величине: минус меняет скидку
 * на наценку, и говорить про «больше суммы позиций» о нём бессмысленно.
 *
 * Скидка со знаком минус прибавляла к итогу, наценка со знаком минус
 * вычитала: «Итого» расходилось с набранным, и ни одна строка экрана
 * этого не объясняла.
 */
fun changeBlockOf(state: SaleState): SaleBlock? {
    val discount = state.discount.sumOf(state.itemsSum) ?: BigDecimal.ZERO
    if (state.hasItemDiscount && discount > BigDecimal.ZERO) return SaleBlock.DiscountScopes
    if (negative(state.discount) || negative(state.markup)) return SaleBlock.DiscountNegative
    if (overHundred(state.discount) || overHundred(state.markup)) return SaleBlock.PercentOverHundred
    // Сравнение только при набранной скидке: у чека из одних сторно
    // сумма позиций уходит ниже нуля, и ненабранная скидка оказывалась
    // «больше» её. Кассир читал «уменьшите скидку» над двумя пустыми
    // полями, а настоящая помеха — неположительный итог — молчала.
    if (discount.signum() > 0 && discount > state.itemsSum) return SaleBlock.DiscountOverItems
    return null
}

/** Набрано ли число меньше нуля: пустое и недобранное — не минус. */
private fun negative(change: Adjustment): Boolean {
    val entered = change.entered ?: return false
    return entered < BigDecimal.ZERO
}

/** Набран ли процент больше ста: у суммы такой границы нет, у доли есть. */
private fun overHundred(change: Adjustment): Boolean =
    change.unit == AdjustmentUnit.Percent && (change.entered ?: BigDecimal.ZERO) > HUNDRED_PERCENT

private fun takenTooSmall(state: SaleState): Boolean {
    val cash = state.cashSum ?: state.total
    if (cash.signum() <= 0) return false
    val taken = state.taken ?: return false
    return taken < cash
}

/** Ровно столько цифр в ИИН и в БИН. */
const val BIN_LENGTH: Int = 12
