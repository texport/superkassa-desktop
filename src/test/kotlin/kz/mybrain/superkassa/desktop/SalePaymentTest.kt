package kz.mybrain.superkassa.desktop

import kotlinx.serialization.json.Json
import kz.mybrain.superkassa.desktop.server.DictionaryEntry
import kz.mybrain.superkassa.desktop.ui.sale.SaleBlock
import kz.mybrain.superkassa.desktop.ui.sale.SaleState
import kz.mybrain.superkassa.desktop.ui.sale.blockOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Допустимость вида оплаты объявляет узел, а не приложение.
 *
 * Ответ ниже снят с работающего узла на `GET /dictionaries/payment-types`
 * 1 сентября 2026 года: кредит и тара в справочнике есть, но помечены
 * непринимаемыми. Свой список «плохих кодов» в приложении разошёлся бы
 * с узлом на первой же смене версии протокола.
 */
class SalePaymentTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun refusedByNode(): Set<String> = json
        .decodeFromString<List<DictionaryEntry>>(NODE_ANSWER)
        .filterNot { it.supported }
        .map { it.code }
        .toSet()

    @Test
    fun `непринимаемые виды берутся из ответа узла`() {
        assertEquals(setOf("CREDIT", "TARE"), refusedByNode())
    }

    @Test
    fun `выбранный непринимаемый вид не даёт пробить чек`() {
        val refused = refusedByNode()
        assertEquals(
            SaleBlock.PaymentUnsupported,
            blockOf(SaleState(paymentCodes = listOf("CREDIT"), unsupportedPayments = refused))
        )
        assertNull(blockOf(SaleState(paymentCodes = listOf("CARD"), unsupportedPayments = refused)))
    }

    @Test
    fun `разрешённый узлом вид приложение не запрещает само`() {
        assertNull(blockOf(SaleState(paymentCodes = listOf("CREDIT"), unsupportedPayments = emptySet())))
    }

    @Test
    fun `вид без признака считается принимаемым`() {
        val entry = json.decodeFromString<DictionaryEntry>("""{"code":"CASH"}""")
        assertTrue(entry.supported)
    }
}

private const val NODE_ANSWER = """
[
  {"code":"CASH","name":{"ru":"Наличные средства","en":"Cash"},"supported":true},
  {"code":"CARD","name":{"ru":"Платежная карта","en":"Payment Card"},"supported":true},
  {"code":"ELECTRONIC","name":{"ru":"Электронные деньги","en":"Electronic Money"},"supported":true},
  {"code":"MOBILE","name":{"ru":"Мобильный платеж (QR)","en":"Mobile Payment (QR)"},"supported":true},
  {"code":"CREDIT","name":{"ru":"Оплата в кредит","en":"Credit Payment"},"supported":false},
  {"code":"TARE","name":{"ru":"Оплата тарой","en":"Payment by Tare"},"supported":false}
]
"""
