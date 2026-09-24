package kz.mybrain.superkassa.strings.api.kassa

import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kz.mybrain.superkassa.strings.impl.kassa.saleTextsEn
import kz.mybrain.superkassa.strings.impl.kassa.saleTextsKk
import kz.mybrain.superkassa.strings.impl.kassa.saleTextsRu
import kz.mybrain.superkassa.strings.lines
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

    private fun values(texts: SaleTexts): Map<String, String> = lines(texts).toMap()

    @Test
    fun `ни одна надпись не пуста ни на одном языке`() {
        Language.entries.forEach { language ->
            values(textsOf(language).kassa.sale).forEach { (name, text) ->
                assertTrue(text.isNotBlank(), "$name пуст на ${language.code}")
            }
        }
    }

    @Test
    fun `каждая надпись переведена — а не скопирована`() {
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
        assertEquals(saleTextsKk, textsOf(Language.Kk).kassa.sale)
        assertEquals(saleTextsRu, textsOf(Language.Ru).kassa.sale)
        assertEquals(saleTextsEn, textsOf(Language.En).kassa.sale)
    }
}
