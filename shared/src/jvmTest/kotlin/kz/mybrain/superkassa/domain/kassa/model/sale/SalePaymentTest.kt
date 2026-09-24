package kz.mybrain.superkassa.domain.kassa.model.sale

import kz.mybrain.superkassa.domain.kassa.model.payment.unsupportedPayments
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Непринимаемый вид оплаты не даёт пробить чек, а принимаемый приложение
 * само не запрещает.
 *
 * Кредит и тару справочник кассы называет, но помечает непринимаемыми:
 * выбранный такой вид останавливает чек до отправки в БФД, а не отказом
 * после неё.
 */
class SalePaymentTest {

    private val refused = setOf("CREDIT", "TARE")

    @Test
    fun `выбранный непринимаемый вид не даёт пробить чек`() {
        assertEquals(
            SaleBlock.PaymentUnsupported,
            blockOf(SaleState(paymentCodes = listOf("CREDIT"), unsupportedPayments = refused))
        )
        assertNull(blockOf(SaleState(paymentCodes = listOf("CARD"), unsupportedPayments = refused)))
    }

    @Test
    fun `разрешённый вид приложение не запрещает само`() {
        assertNull(blockOf(SaleState(paymentCodes = listOf("CREDIT"), unsupportedPayments = emptySet())))
    }
}
