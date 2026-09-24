package kz.mybrain.superkassa.presentation.words.kassa

import kz.mybrain.superkassa.domain.kassa.model.sale.DomainField
import kz.mybrain.superkassa.domain.kassa.model.sale.ExciseRefusal
import kz.mybrain.superkassa.domain.kassa.model.sale.SaleBlock
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
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

    private val sale = textsOf(Language.Ru).kassa.sale
    private val payment = textsOf(Language.Ru).kassa.payment

    @Test
    fun `каждая причина названа словами на всех трёх языках`() {
        Language.entries.forEach { language ->
            SaleBlock.entries.forEach { block ->
                val words = block.reason(textsOf(language).kassa.sale, textsOf(language).kassa.payment)
                assertTrue(words.isNotBlank(), "причина ${block.name} без текста: $language")
            }
        }
    }

    @Test
    fun `незаполненный реквизит назван поимённо, а числовое поле — числом`() {
        val words = SaleBlock.DomainFields.reason(sale, payment, DomainField.CarNumber)
        assertTrue(words.contains(sale.carNumber), "причина не называет поле: $words")
        // Числовому полю сказано, что от него нужно число: «Заполните: Тариф»
        // над полем со словом «Городской» — загадка, а не причина.
        val fee = SaleBlock.DomainFields.reason(sale, payment, DomainField.Fee)
        assertTrue(fee.contains(sale.fee), "причина не называет тариф: $fee")
        assertTrue(fee != words)
    }

    @Test
    fun `отказ в марке назван своими словами`() {
        assertEquals(sale.exciseTooLong, ExciseRefusal.TooLong.words(sale))
        assertEquals(sale.exciseRepeated, ExciseRefusal.Repeated.words(sale))
    }
}
