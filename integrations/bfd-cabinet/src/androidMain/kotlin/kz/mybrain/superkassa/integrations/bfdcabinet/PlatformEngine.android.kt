package kz.mybrain.superkassa.integrations.bfdcabinet

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.Dns
import java.net.InetAddress

internal actual fun platformEngine(pin: HostPin?): HttpClientEngine = OkHttp.create {
    if (pin != null) config { dns(PinnedDns(pin)) }
}

/** Имя из [pin] — по его IP, остальные имена — как находит сеть. */
private class PinnedDns(private val pin: HostPin) : Dns {
    override fun lookup(hostname: String): List<InetAddress> =
        if (hostname.equals(pin.host, ignoreCase = true)) {
            listOf(InetAddress.getByName(pin.address))
        } else {
            Dns.SYSTEM.lookup(hostname)
        }
}
