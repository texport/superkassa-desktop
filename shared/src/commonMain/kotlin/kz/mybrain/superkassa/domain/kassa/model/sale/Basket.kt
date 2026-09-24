package kz.mybrain.superkassa.domain.kassa.model.sale

import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptItemRequest
import kz.mybrain.superkassa.domain.kassa.model.Tenge

/**
 * Корзина чека.
 *
 * Неизменяемая: правка возвращает новую корзину, и живёт она в модели
 * продажи, а не в памяти экрана — уход в другой раздел её не стирает.
 * Суммы — целые тиыны и то же правило строки, что у кассы: итог экрана
 * и итог чека обязаны совпасть до тиына.
 */
data class Basket(val positions: List<Position> = emptyList()) {

    fun add(position: Position): Basket = copy(positions = positions + position)

    /**
     * Сторнирует позицию, уже стоящую в чеке.
     *
     * Сторно — отмена того, что пробито, а не отдельный товар: кассир
     * выбирает строку и отменяет её. Пока чек не пробит, отметка —
     * черновик, и снимается тем же нажатием.
     */
    fun stornoAt(index: Int): Basket = edit(index) { it.copy(storno = !it.storno) }

    /**
     * Заменяет акцизные марки позиции.
     *
     * Марки считываются после того, как товар уже в чеке: кассир сперва
     * пробивает бутылку, потом подносит к сканеру её марку.
     */
    fun stampAt(index: Int, stamps: List<String>): Basket = edit(index) { it.copy(exciseStamps = stamps) }

    fun removeAt(index: Int): Basket =
        if (index in positions.indices) copy(positions = positions.filterIndexed { i, _ -> i != index }) else this

    /** Сумма позиций в тиынах: сторно уводит её вниз. */
    val total: Long get() = positions.sumOf { it.total }

    /**
     * Есть ли в чеке скидка на позицию.
     *
     * Касса отвечает RECEIPT_DISCOUNT_SCOPES_CONFLICT на чек, где скидка
     * стоит и на позиции, и на всём чеке. Кассир обязан узнать об этом
     * до нажатия, а не из отказа.
     */
    val hasItemDiscount: Boolean get() = positions.any { it.discount > 0L }

    /**
     * Сколько уже дано скидкой по строкам чека, в тиынах.
     *
     * Сторнированная строка забирает свою скидку обратно тем же знаком,
     * каким забирает стоимость.
     */
    val itemDiscounts: Long get() = positions.sumOf { if (it.storno) -it.discount else it.discount }

    /**
     * Есть ли в чеке строка стоимостью ноль.
     *
     * Нулевая строка фискального чека — не продажа. Считается по стоимости
     * строки, а не по цене: её обнуляет и каталог без цены, и скидка,
     * забравшая строку целиком.
     */
    val hasZeroLine: Boolean get() = positions.any { it.lineSum <= 0L }

    /**
     * Итог со скидкой или наценкой на чек.
     *
     * Скидка и наценка на чек взаимно исключают друг друга, и ниже нуля
     * итог не опускается — так же, как у кассы.
     */
    fun totalWith(discount: Long?, markup: Long?): Long = (total - (discount ?: 0L) + (markup ?: 0L)).coerceAtLeast(0L)

    /**
     * Позиции для кассы: строка чека — теми же ценой, количеством и скидкой.
     *
     * @param vat какая ставка уйдёт у строки по набранной у неё; `null` —
     *   у строки ставки нет, НДС задан на весь чек.
     */
    fun toItems(vat: (String) -> String? = { it }): List<ReceiptItemRequest> =
        positions.map { it.toItem(vat(it.vatGroup)) }

    private fun edit(index: Int, change: (Position) -> Position): Basket =
        if (index in positions.indices) {
            copy(positions = positions.mapIndexed { i, p -> if (i == index) change(p) else p })
        } else {
            this
        }
}

/**
 * Позиция корзины.
 *
 * @property price цена за единицу в тиынах.
 * @property quantity количество; весовой товар — до тысячной.
 * @property discount скидка на позицию в тиынах.
 */
data class Position(
    val name: String,
    val price: Long,
    val quantity: Decimal,
    val vatGroup: String,
    val discount: Long = 0L,
    val storno: Boolean = false,
    /** Код единицы измерения ИС ЭСФ: без него касса ставит штуку. */
    val measureUnitCode: String? = null,
    /**
     * Наименование на казахском: печатается на чеке рядом с русским.
     * В ОФД не уходит — у позиции чека в CPCR одно имя.
     */
    val nameKk: String? = null,
    /** НТИН из справочника: касса передаёт его в ОФД полем `ntin`. */
    val ntin: String? = null,
    /**
     * Штрихкод, по которому товар нашли в справочнике: кассир видит
     * в подробностях строки, что именно отсканировал.
     */
    val barcode: String? = null,
    /** Акцизные марки, считанные с товара, по марке на единицу. */
    val exciseStamps: List<String> = emptyList()
) {
    /**
     * Стоимость позиции без учёта направления: цена × количество к
     * ближайшему тиыну, минус скидка — ровно как её посчитает касса.
     *
     * Считает сама касса, её правилом: половина тиына идёт вверх, и 333,33
     * за 1,5 кг — это 500,00. Экран, усекавший долю, показывал 499,99, итог
     * и оплата расходились с чеком на тиын, и касса такой чек отвергала.
     */
    val lineSum: Long get() = (Tenge.lineSum(price, quantity) - discount).coerceAtLeast(0L)

    /** Стоимость со знаком: сторно уводит итог вниз. */
    val total: Long get() = if (storno) -lineSum else lineSum

    /** Строка чека для кассы; [vat] — ставка, уходящая у строки. */
    fun toItem(vat: String? = vatGroup): ReceiptItemRequest = ReceiptItemRequest(
        barcode = barcode,
        listExciseStamp = exciseStamps.takeIf { it.isNotEmpty() },
        discountSum = discount.takeIf { it > 0L }?.let(Tenge::decimal),
        measureUnitCode = measureUnitCode,
        name = name,
        nameKk = nameKk,
        ntin = ntin,
        price = Tenge.decimal(price),
        quantity = quantity,
        vatGroup = vat,
        isStorno = storno.takeIf { it }
    )
}
