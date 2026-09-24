package kz.mybrain.superkassa.presentation.kassa.sale

import io.github.texport.superkassa.core.presentation.api.model.kkm.VatGroup
import io.github.texport.superkassa.testing.api.kassa.VatMode
import kz.mybrain.superkassa.domain.kassa.model.sale.SaleOperation
import kz.mybrain.superkassa.kassa.CoreDesk
import kz.mybrain.superkassa.presentation.common.message.Message
import kz.mybrain.superkassa.presentation.strings.common.Language
import kz.mybrain.superkassa.presentation.strings.kassa.refusal.kassaRefusalTexts
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Отказы кассы в продаже на настоящем ядре — словами кассира.
 *
 * Понятные слова ядра идут как есть: о нехватке наличных ядро говорит
 * на языке кассира само. Своя формулировка — там, где ядро отвечает
 * непонятным: на незнакомую ставку НДС оно падает разбором запроса,
 * и кассир читал «нет ответа, проверьте журнал», хотя чек не пробивался.
 */
class SaleRefusalCoreTest {
    private val desk = CoreDesk()

    @AfterTest
    fun close() = desk.close()

    @Test
    fun `покупка на больше, чем в ящике, — слова ядра о наличных`() {
        desk.seated()
        val model = desk.sale()
        model.form.operation(SaleOperation.Buy)
        model.add("Лом цветного металла", "5000")

        model.issue()

        assertIs<Message.Refusal>(desk.said, desk.saidText)
        assertEquals("В кассе недостаточно наличных.", desk.saidText)
    }

    @Test
    fun `незнакомая ставка НДС — свои слова, в БФД ничего не ушло`() {
        desk.seated(VatMode.Payer(VatGroup.VAT_16))
        val model = desk.sale()
        model.entry.editDraft(model.state.value.draft.copy(name = "Кумыс", price = "600", vatGroup = "VAT_99"))
        assertTrue(model.entry.addDraft(), "позиция не встала в чек")

        model.issue()

        assertIs<Message.Refusal>(desk.said, desk.saidText)
        assertEquals(kassaRefusalTexts(Language.Ru).vatUnknown, desk.saidText)
        assertTrue(desk.bfd.countedTickets().isEmpty(), "чек с незнакомой ставкой ушёл в БФД")
    }
}
