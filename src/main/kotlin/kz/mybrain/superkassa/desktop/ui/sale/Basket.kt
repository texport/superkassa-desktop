package kz.mybrain.superkassa.desktop.ui.sale

import androidx.compose.runtime.mutableStateListOf
import kz.mybrain.superkassa.desktop.server.ReceiptItem
import kz.mybrain.superkassa.desktop.ui.components.Money
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Корзина чека.
 *
 * Суммы считаются точной десятичной арифметикой: в чеке расхождение
 * в один тиын — это расхождение с ОФД, а не округление на экране.
 */
class Basket {
    val positions = mutableStateListOf<Position>()

    fun add(position: Position) {
        positions.add(position)
    }

    /**
     * Сторнирует позицию, уже стоящую в чеке.
     *
     * Сторно — отмена того, что пробито, а не отдельный товар: кассир
     * выбирает строку и отменяет её. Прежде сторно ставилось галочкой
     * в форме новой позиции, и в чек уходила вторая строка, набранная
     * заново, — с любой ценой и любым наименованием.
     */
    fun stornoAt(index: Int) {
        val position = positions.getOrNull(index) ?: return
        if (position.storno) return
        positions[index] = position.copy(storno = true)
    }

    fun removeAt(index: Int) {
        if (index in positions.indices) {
            positions.removeAt(index)
        }
    }

    fun clear() = positions.clear()

    val total: BigDecimal
        get() = positions.fold(BigDecimal.ZERO) { sum, position -> sum + position.total }

    /**
     * Есть ли в чеке скидка на позицию.
     *
     * Узел отвечает RECEIPT_DISCOUNT_SCOPES_CONFLICT на чек, где скидка
     * стоит и на позиции, и на всём чеке. Кассир обязан узнать об этом
     * до нажатия, а не из отказа.
     */
    val hasItemDiscount: Boolean
        get() = positions.any { it.discount > BigDecimal.ZERO }

    /**
     * Итог со скидкой или наценкой на чек.
     *
     * Скидка и наценка на чек взаимно исключают друг друга: протокол
     * не допускает обе сразу, и ниже нуля итог не опускается.
     */
    fun totalWith(discount: BigDecimal?, markup: BigDecimal?): BigDecimal {
        val base = total - (discount ?: BigDecimal.ZERO) + (markup ?: BigDecimal.ZERO)
        return if (base < BigDecimal.ZERO) BigDecimal.ZERO.setScale(TIYN_SCALE) else base
    }

    fun toReceiptItems(): List<ReceiptItem> = positions.map { position ->
        ReceiptItem(
            name = position.name,
            nameKk = position.nameKk,
            price = position.price,
            quantity = position.quantity,
            vatGroup = position.vatGroup,
            discountSum = position.discount.takeIf { it > BigDecimal.ZERO },
            measureUnitCode = position.measureUnitCode,
            isStorno = position.storno.takeIf { it }
        )
    }
}

/** Позиция корзины. */
data class Position(
    val name: String,
    val price: BigDecimal,
    val quantity: BigDecimal,
    val vatGroup: String,
    val discount: BigDecimal = BigDecimal.ZERO,
    val storno: Boolean = false,
    /**
     * Код единицы измерения по ОКЕИ.
     *
     * Берётся из справочника вместе с товаром: без него узел ставит штуку,
     * и килограмм баранины уходил в чек как «1 шт».
     */
    val measureUnitCode: String? = null,
    /**
     * Наименование на казахском.
     *
     * Приходит из справочника вместе с товаром и печатается на чеке рядом
     * с русским: чек в Казахстане двуязычный. В ОФД не уходит — у позиции
     * чека в CPCR одно имя, второго поля нет ни в одной версии протокола.
     */
    val nameKk: String? = null
) {
    /** Стоимость позиции без учёта направления: цена × количество − скидка. */
    val lineSum: BigDecimal
        get() = (price.multiply(quantity) - discount).setScale(TIYN_SCALE, RoundingMode.DOWN)

    val total: BigDecimal
        get() = if (storno) lineSum.negate() else lineSum

    /** Подпись позиции в корзине: сторно кассир должен видеть сразу. */
    fun label(stornoCaption: String): String = if (storno) "$stornoCaption · $name" else name
}

/**
 * Сумма со знаком.
 *
 * [Money.format] теряет минус у сумм меньше тенге: целая часть у «−0,50»
 * равна нулю, и знак пропадает вместе с ней. Сторно на полтиына показалось
 * бы кассиру обычной продажей, поэтому знак ставится здесь явно.
 */
fun formatSigned(amount: BigDecimal): String =
    if (amount.signum() < 0) MINUS + Money.format(amount.abs()) else Money.format(amount)

/** Настоящий минус, а не дефис: в сумме он читается как знак, а не как перенос. */
private const val MINUS = "−"
