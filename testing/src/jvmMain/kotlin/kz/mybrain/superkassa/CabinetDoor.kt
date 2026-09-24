package kz.mybrain.superkassa

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kz.mybrain.superkassa.data.cabinet.RemoteCabinet
import kz.mybrain.superkassa.data.cabinet.signing
import kz.mybrain.superkassa.domain.cabinet.model.CabinetCompany
import kz.mybrain.superkassa.domain.cabinet.model.CabinetUser
import kz.mybrain.superkassa.domain.cabinet.port.Signer
import kz.mybrain.superkassa.integrations.bfdcabinet.BfdCabinet
import kz.mybrain.superkassa.integrations.bfdcabinet.CabinetSettings

/**
 * Порты кабинета на модуле поверх обмена проверки [client] — так же
 * собранные, как в приложении, но без модели окна.
 *
 * Вход по ЭЦП требует подписывающего, которого в проверке нет: владелец
 * ставится тем же ответом, каким его поставил бы кабинет на подписанную
 * задачу (см. [SignedInEngine]); подпись заявлений — у [signer].
 */
class SignedCabinet(client: CabinetWire, signer: Signer = NoSigner) {
    val files = KeptFiles()
    private val door = SignedInEngine(client.http.engine)
    private val entering = EnteringSigner(signer)
    val ports = RemoteCabinet(
        BfdCabinet(CabinetSettings(baseUrl = client.baseUrl), signing(entering), engine = door.engine),
        signer,
        files
    )

    /** Владелец вошёл. */
    fun enter(user: CabinetUser = OWNER, company: CabinetCompany = COMPANY) {
        door.owner = user to company
        entering.during { runBlocking { ports.account.signIn() } }
    }

    companion object {
        /** ИИН владельца и БИН его компании: подставные, но казахстанского вида. */
        val OWNER = CabinetUser(id = "u-1", iin = "870101300123", fullName = "Иванов Сергей")
        val COMPANY = CabinetCompany(id = "c-1", bin = "180140000123", name = "ТОО «Пример»")
    }
}

/**
 * Подставной кабинет проверки со входом: вход по подписи он проводит сам,
 * остальное отдаёт обмену [inner] — тому, что задала проверка.
 *
 * Задача входа — всегда одна и та же ([ENTER_PAYLOAD]); подписанную
 * кабинет принимает и называет вошедшим [owner]. Ответы проверки на вход
 * не рассчитаны: они подставляют тело на любой путь.
 */
class SignedInEngine(inner: HttpClientEngine) {
    private val forward = HttpClient(inner) { expectSuccess = false }

    /** Кого кабинет назовёт вошедшим. */
    var owner: Pair<CabinetUser, CabinetCompany> = SignedCabinet.OWNER to SignedCabinet.COMPANY

    val engine = MockEngine { request ->
        when (request.url.encodedPath) {
            CHALLENGE -> respond(TASK, HttpStatusCode.OK, JSON)
            LOGIN -> respond(login(), HttpStatusCode.OK, JSON)
            LOGOUT -> respond("", HttpStatusCode.NoContent)
            else -> relay(request)
        }
    }

    private suspend fun io.ktor.client.engine.mock.MockRequestHandleScope.relay(request: HttpRequestData) =
        forward.request(request.url) {
            method = request.method
            request.headers.forEach { name, values ->
                if (name !in UNSAFE) values.forEach { headers.append(name, it) }
            }
            setBody(request.body)
        }.let { answer -> respond(answer.bodyAsBytes(), answer.status, answer.headers) }

    private fun login(): String {
        val (user, company) = owner
        return """{"accessToken":"rig-access","user":{"id":"${user.id}","iin":"${user.iin}",""" +
            """"fullName":"${user.fullName}"},"company":{"id":"${company.id}","bin":"${company.bin}",""" +
            """"name":"${company.name.replace("\"", "\\\"")}"}}"""
    }

    private companion object {
        const val CHALLENGE = "/api/auth/eds/challenge"
        const val LOGIN = "/api/auth/eds"
        const val LOGOUT = "/api/auth/logout"
        const val TASK = """{"challengeId":"rig-challenge","payload":"$ENTER_PAYLOAD"}"""
        val JSON = headersOf(HttpHeaders.ContentType, "application/json")
        val UNSAFE = setOf(HttpHeaders.ContentType, HttpHeaders.ContentLength, HttpHeaders.TransferEncoding)
    }
}

/** Задача входа подставного кабинета. */
const val ENTER_PAYLOAD = "cmlnLWVudGVy"

/**
 * Подписывающий проверки: на время [during] задачу входа подписывает сам,
 * иначе — тот, кого задала проверка.
 *
 * Так владелец входит без NCALayer, а проверки подписи видят настоящего
 * подписывающего — и при входе, и при подаче заявления.
 */
class EnteringSigner(private val signer: Signer) : Signer {
    @Volatile
    private var entering = false

    fun during(enter: () -> Unit) {
        entering = true
        try {
            enter()
        } finally {
            entering = false
        }
    }

    override suspend fun sign(payload: String): String =
        if (entering && payload == ENTER_PAYLOAD) "rig-signature" else signer.sign(payload)
}
