package kz.mybrain.superkassa.presentation.kassa.sale.position

import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptItemView
import kz.mybrain.superkassa.domain.kassa.model.Tenge
import kz.mybrain.superkassa.domain.kassa.model.entry.QUANTITY_SCALE
import kz.mybrain.superkassa.domain.kassa.model.sale.Position
import kz.mybrain.superkassa.presentation.common.format.Money
import kz.mybrain.superkassa.strings.api.common.ReceiptTexts
import kz.mybrain.superkassa.strings.api.kassa.SaleTexts

/**
 * Всё, что касса знает о строке чека.
 *
 * Строка бывает двух родов — набранная в корзине и проданная по чеку,
 * который сейчас возвращают, — и сведения о них приходят разными
 * типами. Подробности же кассир читает одни и те же, поэтому оба рода
 * сводятся сюда, а окно и перечень строк знают только этот тип.
 *
 * Чего у строки нет, то `null` или пусто: у проданной по чеку-основанию
 * нет скидки и НТИН, у набранной руками — штрихкода. Такие строки
 * в подробностях не показываются вовсе, а не стоят прочерком. Деньги —
 * в тиынах.
 */
internal data class PositionDetails(
    val name: String,
    val nameKk: String? = null,
    val price: Long,
    val quantity: Decimal,
    val measureUnitCode: String? = null,
    /** Стоимость строки со знаком: сторно уводит итог вниз и показывается с минусом. */
    val sum: Long,
    val discount: Long? = null,
    val vatGroup: String? = null,
    val ntin: String? = null,
    val barcode: String? = null,
    val exciseStamps: List<String> = emptyList(),
    val storno: Boolean = false
)

/** Подробности строки корзины. */
internal fun Position.details(): PositionDetails = PositionDetails(
    name = name,
    nameKk = nameKk,
    price = price,
    quantity = quantity,
    measureUnitCode = measureUnitCode,
    sum = total,
    discount = discount,
    vatGroup = vatGroup,
    ntin = ntin,
    barcode = barcode,
    exciseStamps = exciseStamps,
    storno = storno
)

/**
 * Подробности строки чека-основания.
 *
 * Касса отдаёт количество в тысячных долях — так его везёт протокол, —
 * а кассир читает килограммы, поэтому оно переводится тем же порядком,
 * что и при сборке строк возврата.
 */
internal fun ReceiptItemView.details(): PositionDetails = PositionDetails(
    name = name,
    nameKk = nameKk,
    price = Tenge.truncated(price),
    quantity = Decimal.ofScaled(quantityThousandths, QUANTITY_SCALE),
    measureUnitCode = measureUnitCode,
    sum = Tenge.truncated(sum).let { if (isStorno) -it else it },
    vatGroup = vatGroup,
    barcode = barcode,
    storno = isStorno
)

/**
 * Строки окна подробностей: подпись и значение, сверху вниз.
 *
 * Порядок — как на бумажном чеке: наименование, цена, количество,
 * сумма, потом налог и коды. Пустые сведения выпадают здесь, а не
 * прячутся в окне: список строк проверяется без экрана.
 *
 * Величины набираются теми же форматтерами, что и в корзине: сумма
 * в подробностях, не совпавшая с суммой в строке, читалась бы как
 * расхождение в чеке.
 */
internal fun PositionDetails.rows(
    labels: ReceiptTexts,
    texts: SaleTexts,
    units: List<MeasureUnit>,
    rates: List<VatRate>
): List<Pair<String, String>> = listOfNotNull(
    labels.name to name,
    nameKk?.takeIf { it.isNotBlank() }?.let { texts.positionNameKk to it },
    labels.price to Money.formatTiyn(price),
    labels.quantity to countedText(units),
    texts.positionSum to Money.formatTiyn(sum),
    discount?.takeIf { it > 0L }?.let { labels.discount to Money.formatTiyn(it) },
    vatGroup?.takeIf { it.isNotBlank() && rates.isNotEmpty() }?.let { labels.vat to vatTitle(rates, it) },
    ntin?.takeIf { it.isNotBlank() }?.let { texts.positionNtin to it },
    barcode?.takeIf { it.isNotBlank() }?.let { labels.barcode to it },
    exciseStamps.takeIf { it.isNotEmpty() }?.let { texts.excise to it.joinToString(STAMP_BREAK) },
    texts.positionStornoMarked.takeIf { storno }?.let { labels.storno to it }
)

/** Количество с единицей: «1,5 кг»; без кода единицы остаётся одно число. */
private fun PositionDetails.countedText(units: List<MeasureUnit>): String =
    listOf(quantityText(quantity), unitTitle(units, measureUnitCode))
        .filter { it.isNotBlank() }
        .joinToString(" ")

/** Марки идут по одной в строке: их сверяют с бутылкой глазами, а подряд они слипаются. */
private const val STAMP_BREAK = "\n"
