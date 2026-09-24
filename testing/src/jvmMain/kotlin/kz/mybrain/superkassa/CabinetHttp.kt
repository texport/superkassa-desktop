package kz.mybrain.superkassa

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kz.mybrain.superkassa.domain.cabinet.model.EdsProblem
import kz.mybrain.superkassa.domain.cabinet.model.EdsRefusal
import kz.mybrain.superkassa.domain.cabinet.port.CabinetPorts
import kz.mybrain.superkassa.domain.cabinet.port.SavedFiles
import kz.mybrain.superkassa.domain.cabinet.port.Signer

/** Порты кабинета поверх обмена проверки — от имени вошедшего владельца, без модели окна. */
fun CabinetWire.signedPorts(): CabinetPorts = SignedCabinet(this).also { it.enter() }.ports

/** Обмен, отвечающий на всё одним и тем же. */
fun replying(body: String, status: HttpStatusCode = HttpStatusCode.OK): HttpClient =
    jsonHttp(MockEngine { respond(body, status, headersOf(HttpHeaders.ContentType, "application/json")) })

/** Обмен поверх [engine] так, как его ведёт кабинет: отказ читается телом, а не исключением. */
fun jsonHttp(engine: MockEngine): HttpClient = HttpClient(engine) {
    expectSuccess = false
    install(ContentNegotiation) { json(CabinetWire.json) }
}

/** Подписывающий, которого нет: в проверках без NCALayer подпись не получить. */
object NoSigner : Signer {
    override suspend fun sign(payload: String): String =
        throw EdsRefusal(EdsProblem.Unreachable, Signer.NO_HANDSHAKE)
}

/** Сохранённые файлы — в памяти проверки, без окна выбора. */
class KeptFiles : SavedFiles {
    val saved = mutableMapOf<String, ByteArray>()

    override suspend fun save(bytes: ByteArray, name: String): String {
        saved[name] = bytes
        return name
    }
}
