package kz.mybrain.superkassa.data.cabinet.setup

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kz.mybrain.superkassa.CabinetRig
import kz.mybrain.superkassa.CabinetWire
import kz.mybrain.superkassa.domain.cabinet.port.Signer
import kz.mybrain.superkassa.domain.setup.model.CabinetRecord
import kz.mybrain.superkassa.jsonHttp
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Мастер поверх портов кабинета: какие ручки он зовёт и что берёт из ответов.
 *
 * Своих обращений у мастера нет — он идёт теми же портами, что и разделы
 * кабинета, от имени того же вошедшего.
 */
class CabinetSetupTest {
    private val asked = CopyOnWriteArrayList<String>()
    private val engine = MockEngine { request ->
        asked += "${request.method.value} ${request.url.encodedPath}"
        respond(bodyFor(request.method, request.url.encodedPath), HttpStatusCode.OK, JSON)
    }
    private val rig = CabinetRig(CabinetWire(http = jsonHttp(engine)), signer = SigningOwner).enter()
    private val setup = CabinetSetup(rig.ports)

    @Test
    fun `касса в кабинете — состояние и номер КГД`(): Unit = runBlocking {
        assertEquals(CabinetRecord("REGISTERED", "000000200042"), setup.record("r-1"))
    }

    /** Вошедший кабинет окна сам читает точки и кассы: здесь важны только обращения мастера. */
    @Test
    fun `заявление готовится, подписывается и уходит одной кассе`(): Unit = runBlocking {
        val prepared = setup.prepareRegistration("r-1")
        val signature = setup.sign(prepared.payload)
        setup.sendRegistration("r-1", prepared.actionId, signature)

        assertEquals("a-1", prepared.actionId)
        assertEquals("cms-cGF5bG9hZA==", signature)
        assertEquals(
            listOf(
                "POST /api/cash-registers/r-1/registration/application",
                "POST /api/cash-registers/r-1/registration/sign"
            ),
            asked.filter { it.startsWith("POST") }
        )
    }

    /** Токен без знака: кабинет отдаёт его числом, касса ждёт беззнаковое 32-битное. */
    @Test
    fun `выпущенный токен уходит числом без знака`(): Unit = runBlocking {
        assertEquals("3735928559", setup.issueToken("r-1"))
    }

    private fun bodyFor(method: HttpMethod, path: String): String = when {
        path.endsWith("/registration/application") -> PREPARED
        path.endsWith("/registration/sign") -> SENT
        path.endsWith("/token") && method == HttpMethod.Post -> ISSUED
        else -> REGISTER
    }

    /** Владелец подписывает сразу: проверке нужен ход подачи, а не окно NCALayer. */
    private object SigningOwner : Signer {
        override suspend fun sign(payload: String): String = "cms-$payload"
    }

    private companion object {
        val JSON = headersOf(HttpHeaders.ContentType, "application/json")
        const val REGISTER = """{"id":"r-1","kkmId":5000021,"status":"REGISTERED",""" +
            """"registrationNumber":"000000200042"}"""
        const val PREPARED = """{"actionId":"a-1","actionType":"REGISTRATION","payloadToSign":"cGF5bG9hZA=="}"""
        const val SENT = """{"actionId":"a-1","actionStatus":"SENT"}"""
        const val ISSUED = """{"kkmId":5000021,"token":-559038737,"status":"ISSUED"}"""
    }
}
