package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.app.PrintFileName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Имя файла у документа, сохранённого из кабинета.
 *
 * Кабинет называет виды документов своими словами: `PURCHASE`,
 * `DEPOSIT`, `WITHDRAWAL`. Имя файла их не знало и звало такой документ
 * «document»: покупка, внесение и изъятие сохранялись под одним именем
 * и различались только номером. Владелец складывает эти файлы для КГД
 * и через неделю не отличает один от другого.
 *
 * Список видов — тот же, что журнал кабинета переводит на слова
 * владельца (`documentTitle`); имя файла обязано знать ровно его.
 */
class PrintFileNameCabinetTest {

    @Test
    fun `виды документов кабинета называются в имени файла своими словами`() {
        val expected = mapOf(
            "SALE" to "receipt-sale",
            "RETURN" to "receipt-sale-return",
            "BUY" to "receipt-buy",
            "PURCHASE" to "receipt-buy",
            "BUY_RETURN" to "receipt-buy-return",
            "PURCHASE_RETURN" to "receipt-buy-return",
            "DEPOSIT" to "cash-in",
            "WITHDRAWAL" to "cash-out",
            "X" to "x-report",
            "Z" to "z-report"
        )

        expected.forEach { (code, kind) ->
            assertEquals(kind, PrintFileName.of(code, number = null, shiftNo = null), "вид $code")
        }
    }

    @Test
    fun `номер и смена дополняют имя, а незнакомый вид остаётся документом`() {
        assertEquals("cash-in-shift-3-77", PrintFileName.of("DEPOSIT", number = "77", shiftNo = 3))
        assertTrue(PrintFileName.of("СОВСЕМ_ДРУГОЕ", number = null, shiftNo = null).startsWith("document"))
    }
}
