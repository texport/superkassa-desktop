package kz.mybrain.superkassa.presentation.journal.documents

import io.github.texport.superkassa.core.presentation.api.model.delivery.ReceiptDeliveryState
import io.github.texport.superkassa.core.presentation.api.model.receipt.CustomerContactRequest
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptResponse
import io.github.texport.superkassa.delivery.api.model.DeliveryChannel
import io.github.texport.superkassa.delivery.api.model.DeliveryRequest
import io.github.texport.superkassa.delivery.api.model.DeliveryResult
import io.github.texport.superkassa.delivery.api.port.DeliveryPort
import io.github.texport.superkassa.testing.api.bfd.FakeBfd
import io.github.texport.superkassa.testing.api.kassa.ReadyKassa
import io.github.texport.superkassa.testing.api.kassa.TestBench
import kotlinx.coroutines.Dispatchers
import kz.mybrain.superkassa.data.kassa.EmbeddedKassa
import kz.mybrain.superkassa.data.kassa.delivery.EmbeddedDeliveries
import kz.mybrain.superkassa.domain.journal.port.JournalPorts
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.appBench
import kz.mybrain.superkassa.kassa.appKassa
import kz.mybrain.superkassa.kassa.orderDelivery
import kz.mybrain.superkassa.presentation.common.message.Notices
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.io.path.createTempDirectory
import kotlin.time.Duration.Companion.hours

/**
 * Касса на настоящем ядре, у которой чек уходит покупателю по SMS.
 *
 * SMS подменный: [sms] отказывает или доставляет, как велит проверка,
 * и помнит, что ему отдали. Доставка заказывается настройками ядра до
 * открытия кассы ([orderDelivery]), а чек уходит на контакт покупателя
 * из самого чека ([sale]). Касса — с настройками приложения. За кассой
 * сидит кассир.
 *
 * @param ordered заказана ли доставка чека вообще.
 */
internal class DeliveryBench(ordered: Boolean = true) : AutoCloseable {
    private val directory: File = createTempDirectory("kassa-delivery-").toFile()
    val sms = TestSms()
    val bench: TestBench
    val kassa: ReadyKassa
    val signIn = SignIn()
    val notices = Notices()

    init {
        val bfd = FakeBfd()
        if (ordered) orderDelivery(directory, bfd, listOf(sms))
        bench = appBench(directory, bfd, channels = listOf(sms))
        kassa = bench.registerKassa(appKassa(adminPin = ADMIN, cashierPin = CASHIER)).also { it.openShift() }
        signIn.enter(kassa.info(), bench.api.authenticate(kassa.kkmId, CASHIER), CASHIER)
    }

    /** Продажа с контактом покупателя: чек уходит ему по SMS. */
    fun sale(): ReceiptResponse = kassa.sell(buyer = BUYER_CONTACT)

    /** Модель журнала, как её собирает окно: касса и доставка — порты приложения. */
    fun model(): JournalViewModel {
        val deliveries = JournalPorts(EmbeddedDeliveries(bench.superkassa.delivery, Dispatchers.Unconfined))
        val app = CoreScene.app(EmbeddedKassa(bench.api, Dispatchers.Unconfined), signIn, notices, journal = deliveries)
        return journalModel(app)
    }

    /** Заходы фоновой доставки, пока попытки не кончатся: доставка [receipt] становится окончательным отказом. */
    fun failForGood(receipt: ReceiptResponse) {
        repeat(MOST_ATTEMPTS) {
            if (kassa.deliveries(receipt).all { it.state == ReceiptDeliveryState.FAILED }) return
            kassa.deliverReceipts()
            kassa.clock.move(1.hours)
        }
        error("доставка не стала окончательным отказом за $MOST_ATTEMPTS заходов")
    }

    override fun close() {
        bench.close()
        directory.deleteRecursively()
    }

    companion object {
        const val ADMIN = "7391"
        const val CASHIER = "4826"

        /** Больше попыток доставки, чем у ядра по умолчанию. */
        private const val MOST_ATTEMPTS = 10

        /** Номер покупателя: в журнал он не попадает. */
        const val BUYER = "+77010000000"

        /** Контакт покупателя в чеке: по нему чек уходит по SMS. */
        val BUYER_CONTACT = CustomerContactRequest(phone = BUYER)
    }
}

/** SMS проверки: отказывает причиной [failing] или доставляет, когда её нет. */
internal class TestSms : DeliveryPort {
    @Volatile
    var failing: String? = "provider unreachable"
    val sent: MutableList<DeliveryRequest> = CopyOnWriteArrayList()

    override val channel: DeliveryChannel = DeliveryChannel.SMS

    override fun send(request: DeliveryRequest): DeliveryResult {
        sent += request
        val reason = failing ?: return DeliveryResult(ok = true)
        return DeliveryResult(ok = false, message = reason, code = FAILURE)
    }

    companion object {
        const val FAILURE = "DELIVERY_SMS_FAILED"
    }
}
