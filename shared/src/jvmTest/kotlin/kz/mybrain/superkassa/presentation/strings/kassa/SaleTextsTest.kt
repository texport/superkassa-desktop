package kz.mybrain.superkassa.presentation.strings.kassa

import kz.mybrain.superkassa.presentation.strings.common.Language
import kotlin.test.Test
import kotlin.test.assertEquals
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
}
