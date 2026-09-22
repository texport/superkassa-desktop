package kz.mybrain.superkassa.desktop.ui.sale

import kz.mybrain.superkassa.desktop.server.SoldItem
import kz.mybrain.superkassa.desktop.server.UnitOfMeasurement
import kz.mybrain.superkassa.desktop.ui.components.Money
import kz.mybrain.superkassa.desktop.ui.strings.SaleStrings
import kz.mybrain.superkassa.desktop.ui.strings.SaleTexts
import java.math.BigDecimal

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
 * в подробностях не показываются вовсе, а не стоят прочерком.
 */
data class PositionDetails(
    val name: String,
    val nameKk: String? = null,
    val price: BigDecimal,
    val quantity: BigDecimal,
    val measureUnitCode: String? = null,
    /** Стоимость строки со знаком: сторно уводит итог вниз и показывается с минусом. */
    val sum: BigDecimal,
    val discount: BigDecimal? = null,
    val vatGroup: String? = null,
    val ntin: String? = null,
    val barcode: String? = null,
    val sectionCode: String? = null,
    val exciseStamps: List<String> = emptyList(),
    val storno: Boolean = false
)

/** Подробности строки корзины. */
fun Position.details(): PositionDetails = PositionDetails(
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
    sectionCode = sectionCode,
    exciseStamps = exciseStamps,
    storno = storno
)

/**
 * Подробности строки чека-основания.
 *
 * Узел отдаёт количество в тысячных долях — так его везёт протокол, —
 * а кассир читает килограммы, поэтому оно переводится тем же порядком,
 * что и при сборке строк возврата.
 */
fun SoldItem.details(): PositionDetails = PositionDetails(
    name = name,
    nameKk = nameKk,
    price = price,
    quantity = BigDecimal.valueOf(quantityThousandths, QUANTITY_SCALE),
    measureUnitCode = measureUnitCode,
    sum = if (isStorno) sum.negate() else sum,
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
fun PositionDetails.rows(
    labels: SaleStrings,
    texts: SaleTexts,
    units: List<UnitOfMeasurement>,
    rates: List<VatRate>
): List<Pair<String, String>> = listOfNotNull(
    labels.name to name,
    nameKk?.takeIf { it.isNotBlank() }?.let { texts.positionNameKk to it },
    labels.price to Money.format(price),
    labels.quantity to countedText(units),
    texts.positionSum to Money.format(sum),
    discount?.takeIf { it > BigDecimal.ZERO }?.let { labels.discount to Money.format(it) },
    vatGroup?.takeIf { it.isNotBlank() }?.let { labels.vat to vatTitle(rates, it) },
    ntin?.takeIf { it.isNotBlank() }?.let { texts.positionNtin to it },
    barcode?.takeIf { it.isNotBlank() }?.let { labels.barcode to it },
    sectionCode?.takeIf { it.isNotBlank() }?.let { texts.positionSection to it },
    exciseStamps.takeIf { it.isNotEmpty() }?.let { texts.excise to it.joinToString(STAMP_BREAK) },
    texts.positionStornoMarked.takeIf { storno }?.let { labels.storno to it }
)

/** Количество с единицей: «1,5 кг»; без кода единицы остаётся одно число. */
private fun PositionDetails.countedText(units: List<UnitOfMeasurement>): String =
    listOf(quantityText(quantity), unitTitle(units, measureUnitCode))
        .filter { it.isNotBlank() }
        .joinToString(" ")

/** Марки идут по одной в строке: их сверяют с бутылкой глазами, а подряд они слипаются. */
private const val STAMP_BREAK = "\n"
