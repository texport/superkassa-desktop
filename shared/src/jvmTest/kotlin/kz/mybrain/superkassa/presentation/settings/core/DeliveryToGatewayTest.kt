package kz.mybrain.superkassa.presentation.settings.core

import com.sun.net.httpserver.HttpServer
import io.github.texport.superkassa.core.presentation.api.model.delivery.ReceiptDeliveryState
import io.github.texport.superkassa.core.presentation.api.model.receipt.CustomerContactRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kz.mybrain.superkassa.domain.settings.model.DeliveryChannel
import kz.mybrain.superkassa.domain.settings.model.DeliveryField
import kz.mybrain.superkassa.presentation.settings.SettingsBench
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Канал, настроенный с карточки, на деле отправляет чек: SMS-шлюз
 * на этой машине принимает обращение кассы с номером покупателя из чека
 * и ключом. Получателя у канала нет — номер приходит с чеком.
 *
 * Канал собирает само ядро из своих настроек — подменного канала в
 * настройках запуска нет. Включение канала ядро читает
 * при запуске, поэтому касса перезапускается между настройкой и чеком.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DeliveryToGatewayTest {
    private lateinit var desk: SettingsBench
    private val gateway: HttpServer = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    private val calls = CopyOnWriteArrayList<Pair<String, String?>>()

    @BeforeTest
    fun open() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        gateway.createContext("/send") { exchange ->
            calls += URLDecoder.decode(exchange.requestURI.rawQuery, Charsets.UTF_8) to
                exchange.requestHeaders.getFirst("Authorization")
            exchange.sendResponseHeaders(OK, -1)
            exchange.close()
        }
        gateway.start()
        desk = SettingsBench().enter()
    }

    @AfterTest
    fun close() {
        desk.close()
        gateway.stop(0)
        Dispatchers.resetMain()
    }

    @Test
    fun `чек уходит на SMS-шлюз, настроенный с карточки`() {
        val model = deliveryModel(desk.app.services, desk.app.areas.settings)
        model.switch(DeliveryChannel.Sms, true)
        model.type(DeliveryField.SmsUrl, "http://127.0.0.1:${gateway.address.port}/send?to={phone}&text={text}")
        model.type(DeliveryField.SmsKey, KEY)
        model.save()

        val kassa = desk.restart().kassa(desk.kassa.kkmId, SettingsBench.ADMIN_PIN, SettingsBench.CASHIER_PIN)
        kassa.openShift()
        val receipt = kassa.sell(buyer = CustomerContactRequest(phone = PHONE))
        kassa.deliverReceipts()

        val (query, auth) = calls.single()
        assertTrue(query.startsWith("to=$PHONE&"), "шлюз получил не тот номер: $query")
        assertEquals("Bearer $KEY", auth)
        val sms = kassa.deliveries(receipt).single { it.channel == "SMS" }
        assertEquals(ReceiptDeliveryState.DELIVERED, sms.state, "журнал доставки: $sms")
        assertFalse(desk.journal.lines.any { it.contains(KEY) || it.contains(PHONE) }, "ключ или номер в журнале")
    }

    private companion object {
        const val OK = 200
        const val PHONE = "+77010000001"
        const val KEY = "sms-key-31c7"
    }
}
