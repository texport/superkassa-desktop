package kz.mybrain.superkassa.domain.kassa.model.entry

import kz.mybrain.superkassa.domain.kassa.model.decimal
import kz.mybrain.superkassa.domain.kassa.model.sale.Basket
import kz.mybrain.superkassa.domain.kassa.model.sale.Position
import kz.mybrain.superkassa.domain.kassa.model.sale.SaleBlock
import kz.mybrain.superkassa.domain.kassa.model.sale.SaleState
import kz.mybrain.superkassa.domain.kassa.model.sale.blockOf
import kz.mybrain.superkassa.domain.kassa.model.tenge
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Позиция каталога, у которой цены нет.
 *
 * Национальный каталог цен не несёт, и найденная в нём позиция вставала
 * в чек нулевой и молча: ни предупреждения, ни требования задать цену.
 * Здесь проверяется обратное — цену спрашивают, отказ ничего не добавляет,
 * а нулевая строка не даёт пробить чек.
 */
class SalePriceAskTest {

    /** Карточка НТИН без цены: наименование, код и килограммы от каталога. */
    private val weighed = Position(
        name = "Сыр полутвёрдый «Қазақ»",
        price = 0L,
        quantity = decimal("1"),
        vatGroup = "VAT_16",
        measureUnitCode = "166",
        nameKk = "Жартылай қатты ірімшік",
        ntin = "KZ01234567890123"
    )

    /** То же, но штуками: у «шт» дробного количества быть не должно. */
    private val piece = weighed.copy(measureUnitCode = "796")

    @Test
    fun `позиция без цены требует вопроса позиция с ценой — нет`() {
        assertTrue(priceMissing(weighed))
        assertFalse(priceMissing(weighed.copy(price = tenge("690.00"))))
    }

    @Test
    fun `количество спрошено с единицы`() {
        assertEquals("1", PriceAsk(weighed).quantity)
    }

    /**
     * Цена разбирается общим разбором денег: кассир вставляет сумму
     * из отчёта, где разряды разделены неразрывным пробелом.
     */
    @Test
    fun `заданная цена и количество становятся позицией`() {
        val asked = PriceAsk(weighed, price = "1\u00A0200,50", quantity = "2")

        val position = assertNotNull(asked.position)

        assertEquals(tenge("1200.50"), position.price)
        assertEquals(decimal("2"), position.quantity)
    }

    /** За наименование, код и единицу отвечает каталог, а не кассир. */
    @Test
    fun `каталожное в позиции остаётся каталожным`() {
        val position = assertNotNull(PriceAsk(weighed, price = "2500").position)

        assertEquals(weighed.name, position.name)
        assertEquals(weighed.ntin, position.ntin)
        assertEquals(weighed.nameKk, position.nameKk)
        assertEquals("166", position.measureUnitCode)
        assertEquals("VAT_16", position.vatGroup)
    }

    @Test
    fun `отказ и незаданная цена не дают ничего для чека`() {
        val untouched = PriceAsk(weighed)

        assertNull(untouched.position)
        assertEquals(DraftProblem.PriceNotPositive, untouched.problem(DraftField.Price))
        assertNull(PriceAsk(weighed, price = "0").position)
        assertNull(PriceAsk(weighed, price = "сто").position)
    }

    @Test
    fun `дробное количество принимает килограмм но не штука`() {
        assertNotNull(PriceAsk(weighed, price = "2500", quantity = "1,45").position)
        assertEquals(
            DraftProblem.QuantityNotWhole,
            PriceAsk(piece, price = "2500", quantity = "1,45").problem(DraftField.Quantity)
        )
        assertNull(PriceAsk(piece, price = "2500", quantity = "1,45").position)
        assertNotNull(PriceAsk(piece, price = "2500", quantity = "3").position)
    }

    /**
     * Нулевая позиция среди оплаченных: итог чека положителен, и общая
     * причина «итог должен быть больше нуля» о ней не сказала бы.
     */
    @Test
    fun `нулевая позиция не даёт пробить чек`() {
        val basket = Basket().add(weighed).add(weighed.copy(name = "Хлеб", price = tenge("249.90")))

        assertTrue(basket.hasZeroLine)
        assertEquals(SaleBlock.ZeroLine, blockOf(SaleState(positions = 2, hasZeroLine = true)))
    }

    @Test
    fun `чек из заполненных позиций помех не находит`() {
        val basket = Basket().add(weighed.copy(price = tenge("2500")))

        assertFalse(basket.hasZeroLine)
        assertNull(blockOf(SaleState(positions = 1, total = tenge("2500"))))
    }
}
