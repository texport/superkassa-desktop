package kz.mybrain.superkassa.presentation.cabinet

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kz.mybrain.superkassa.CabinetRig
import kz.mybrain.superkassa.CabinetWire
import kz.mybrain.superkassa.data.eds.NcaFake
import kz.mybrain.superkassa.data.eds.NcaSigner
import kz.mybrain.superkassa.integrations.ncalayer.DesktopNcaLayer
import kz.mybrain.superkassa.integrations.ncalayer.NcaSettings
import kz.mybrain.superkassa.jsonHttp
import kotlin.time.Duration.Companion.minutes

/**
 * Кабинет, отвечающий одним телом на всё, и подпись у подставного NCALayer.
 *
 * Так проверяется ожидание подписи: кабинет выдал задачу или подготовил
 * заявление, а NCALayer ведёт себя так, как велит проверка, — молчит,
 * отказывает или подписывает.
 *
 * @param asked куда складываются пути обращений: по ним видно, что ушло в кабинет.
 */
internal fun signingRig(fake: NcaFake, body: String, asked: MutableList<String> = mutableListOf()): CabinetRig {
    val engine = MockEngine { request ->
        asked += request.url.encodedPath
        respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
    }
    val layer = DesktopNcaLayer(NcaSettings(fake.address, signWindow = 1.minutes))
    return CabinetRig(CabinetWire(http = jsonHttp(engine)), signer = NcaSigner(layer))
}
