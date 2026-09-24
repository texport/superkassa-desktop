package kz.mybrain.superkassa.presentation.strings.kassa

import kz.mybrain.superkassa.domain.kassa.model.sale.DomainField
import kz.mybrain.superkassa.domain.kassa.model.sale.ExciseRefusal
import kz.mybrain.superkassa.domain.kassa.model.sale.SaleBlock
import kz.mybrain.superkassa.presentation.strings.common.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Причины правил продажи словами кассира.
 *
 * Правило называет причину значением; здесь проверяется, что каждое
 * значение получает слова на каждом из трёх языков и что поле названо
 * поимённо.
 */
class SaleWordsTest {

    @Test
    fun `каждая причина названа словами на всех трёх языках`() {
        Language.entries.forEach { language ->
            SaleBlock.entries.forEach { block ->
                val words = block.reason(saleTexts(language), paymentTexts(language))
                assertTrue(words.isNotBlank(), "причина ${block.name} без текста: $language")
            }
        }
    }

    @Test
    fun `незаполненный реквизит назван поимённо, а числовое поле — числом`() {
        val words = SaleBlock.DomainFields.reason(saleTextsRu, paymentTextsRu, DomainField.CarNumber)
        assertTrue(words.contains(saleTextsRu.carNumber), "причина не называет поле: $words")
        // Числовому полю сказано, что от него нужно число: «Заполните: Тариф»
        // над полем со словом «Городской» — загадка, а не причина.
        val fee = SaleBlock.DomainFields.reason(saleTextsRu, paymentTextsRu, DomainField.Fee)
        assertTrue(fee.contains(saleTextsRu.fee), "причина не называет тариф: $fee")
        assertTrue(fee != words)
    }

    @Test
    fun `отказ в марке назван своими словами`() {
        assertEquals(saleTextsRu.exciseTooLong, ExciseRefusal.TooLong.words(saleTextsRu))
        assertEquals(saleTextsRu.exciseRepeated, ExciseRefusal.Repeated.words(saleTextsRu))
    }
}
