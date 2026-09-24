package kz.mybrain.superkassa.presentation.kassa.sale.position

import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptItemView
import kz.mybrain.superkassa.domain.kassa.model.decimal
import kz.mybrain.superkassa.domain.kassa.model.sale.Position
import kz.mybrain.superkassa.domain.kassa.model.tenge
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Состав строк окна подробностей строки чека.
 *
 * Строки собираются без экрана, и проверяются здесь же: что показано
 * у полной позиции, что скрыто у скупой, и что величины набраны теми
 * же форматтерами, что и в корзине.
 */
class PositionDetailsTest {

    private val labels = textsOf(Language.Ru).common.sale
    private val texts = textsOf(Language.Ru).kassa.sale
    private val units = measureUnits(Language.Ru)
    private val rates = listOf(VatRate("NO_VAT", "Без НДС"), VatRate("VAT_16", "НДС", percent = 16))

    private val full = Position(
        name = "Коньяк «Казахстан» 0,5 л",
        nameKk = "«Қазақстан» коньягы 0,5 л",
        price = tenge("12500"),
        quantity = decimal("2"),
        vatGroup = "VAT_16",
        discount = tenge("500"),
        measureUnitCode = "796",
        ntin = "KZ01234567890123",
        barcode = "4870001234567",
        exciseStamps = listOf("KZ0000000001", "KZ0000000002")
    )

    private fun rowsOf(position: Position) = position.details().rows(labels, texts, units, rates)

    @Test
    fun `полная позиция показывает каждое поле в порядке чека`() {
        val rows = rowsOf(full)

        assertEquals(
            listOf(
                labels.name, texts.positionNameKk, labels.price, labels.quantity, texts.positionSum,
                labels.discount, labels.vat, texts.positionNtin, labels.barcode, texts.excise
            ),
            rows.map { it.first }
        )
        val byLabel = rows.toMap()
        assertEquals("12 500,00 ₸", byLabel[labels.price]?.plain())
        assertEquals("2 шт", byLabel[labels.quantity])
        assertEquals("24 500,00 ₸", byLabel[texts.positionSum]?.plain())
        assertEquals("500,00 ₸", byLabel[labels.discount]?.plain())
        assertEquals("НДС 16%", byLabel[labels.vat])
        assertEquals("KZ0000000001\nKZ0000000002", byLabel[texts.excise])
    }

    @Test
    fun `у набранной руками позиции пустые поля скрыты`() {
        val rows = rowsOf(
            Position(name = "Пакет", price = tenge("15"), quantity = decimal("1"), vatGroup = "NO_VAT")
        )

        assertEquals(
            listOf(labels.name, labels.price, labels.quantity, texts.positionSum, labels.vat),
            rows.map { it.first }
        )
        assertEquals("1", rows.toMap()[labels.quantity])
    }

    /** Сторно показано и словами, и минусом у суммы: строка уводит итог вниз. */
    @Test
    fun `сторно отмечено строкой и знаком суммы`() {
        val rows = rowsOf(full.copy(storno = true)).toMap()

        assertEquals(texts.positionStornoMarked, rows[labels.storno])
        assertEquals("−24 500,00 ₸", rows[texts.positionSum]?.plain())
    }

    /** Строка чека-основания: количество из тысячных долей, штрихкод есть, скидки и НТИН нет. */
    @Test
    fun `проданная строка читается из ответа кассы`() {
        val sold = ReceiptItemView(
            name = "Сыр",
            price = Decimal.parse("3450"),
            quantityThousandths = 1_450,
            sum = Decimal.parse("5002.50"),
            vatGroup = "VAT_16",
            measureUnitCode = "116",
            barcode = "2000000000012"
        )

        val rows = sold.details().rows(labels, texts, units, rates).toMap()

        assertEquals("1,450 кг", rows[labels.quantity])
        assertEquals("5 002,50 ₸", rows[texts.positionSum]?.plain())
        assertEquals("2000000000012", rows[labels.barcode])
        assertNull(rows[labels.discount])
        assertNull(rows[texts.positionNtin])
    }

    /** Неразрывные пробелы разрядов — обычными, чтобы ожидание читалось в исходнике. */
    private fun String.plain(): String = replace(' ', ' ')
}
