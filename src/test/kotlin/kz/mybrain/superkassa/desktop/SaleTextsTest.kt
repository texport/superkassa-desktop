package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.ui.sale.refusalWords
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.SaleTexts
import kz.mybrain.superkassa.desktop.ui.strings.saleTexts
import kz.mybrain.superkassa.desktop.ui.strings.saleTextsEn
import kz.mybrain.superkassa.desktop.ui.strings.saleTextsKk
import kz.mybrain.superkassa.desktop.ui.strings.saleTextsRu
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Надписи области продажи.
 *
 * Касса государственная: экран обязан читаться на казахском так же полно,
 * как на русском. Пустая или забытая строка — это кассир, которому нечего
 * прочитать в момент отказа.
 */
class SaleTextsTest {

    private fun values(texts: SaleTexts): Map<String, String> {
        val fields = SaleTexts::class.java.declaredFields.filter { it.type == String::class.java }
        return fields.associate { field ->
            field.isAccessible = true
            field.name to (field.get(texts) as String)
        }
    }

    @Test
    fun `ни одна надпись не пуста ни на одном языке`() {
        Language.entries.forEach { language ->
            values(saleTexts(language)).forEach { (name, text) ->
                assertTrue(text.isNotBlank(), "$name пуст на ${language.code}")
            }
        }
    }

    @Test
    fun `каждая надпись переведена, а не скопирована`() {
        val ru = values(saleTextsRu)
        val kk = values(saleTextsKk)
        val en = values(saleTextsEn)
        ru.forEach { (name, text) ->
            assertTrue(text != kk[name], "$name не переведён на казахский")
            assertTrue(text != en[name], "$name не переведён на английский")
        }
    }

    @Test
    fun `язык выбирается однозначно`() {
        assertEquals(saleTextsKk, saleTexts(Language.Kk))
        assertEquals(saleTextsRu, saleTexts(Language.Ru))
        assertEquals(saleTextsEn, saleTexts(Language.En))
    }

    @Test
    fun `отказы узла переписываются словами кассира`() {
        assertEquals(saleTextsRu.blockShiftClosed, refusalWords("SHIFT_NOT_OPEN", "", saleTextsRu))
        assertEquals(
            saleTextsRu.blockPaymentUnsupported,
            refusalWords("PAYMENT_TYPE_NOT_SUPPORTED", "", saleTextsRu)
        )
        assertEquals(
            saleTextsRu.blockDiscountScopes,
            refusalWords("RECEIPT_DISCOUNT_SCOPES_CONFLICT", "", saleTextsRu)
        )
        assertEquals(saleTextsRu.refusalNoCash, refusalWords("INSUFFICIENT_CASH", "", saleTextsRu))
    }

    @Test
    fun `английский ответ узла про принятое и ставку переводится`() {
        assertEquals(
            saleTextsRu.blockTakenTooSmall,
            refusalWords("INVALID_ARGUMENT", "taken must be >= sum of CASH payments", saleTextsRu)
        )
        assertEquals(
            saleTextsRu.refusalVatGroup,
            refusalWords("INVALID_ARGUMENT", "Invalid vatGroup: VAT_20", saleTextsRu)
        )
        assertEquals(
            saleTextsRu.blockEmptyBasket,
            refusalWords("VALIDATION_ERROR", "items: Список позиций не может быть пустым", saleTextsRu)
        )
    }

    @Test
    fun `незнакомый отказ не выдумывается`() {
        assertNull(refusalWords("SOMETHING_NEW", "нечто", saleTextsRu))
        assertNull(refusalWords("INVALID_ARGUMENT", "нечто", saleTextsRu))
        assertNotNull(refusalWords("KKM_BLOCKED", "", saleTextsKk))
    }
}
