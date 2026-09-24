package kz.mybrain.superkassa.kassa

import io.github.texport.superkassa.delivery.api.model.DeliveryChannel
import io.github.texport.superkassa.delivery.api.model.DeliveryRequest
import io.github.texport.superkassa.delivery.api.model.DeliveryResult
import io.github.texport.superkassa.delivery.api.port.DeliveryPort
import java.util.concurrent.CopyOnWriteArrayList

/** SMS проверки: отказывает причиной [failing] или доставляет, когда её нет. */
class TestSms : DeliveryPort {
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
