package kz.mybrain.superkassa.data.cabinet

import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kz.mybrain.superkassa.CabinetWire
import kz.mybrain.superkassa.KeptFiles
import kz.mybrain.superkassa.domain.cabinet.model.CabinetExpired
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRefusal
import kz.mybrain.superkassa.domain.cabinet.model.CabinetUnreachable
import kz.mybrain.superkassa.domain.cabinet.model.RegisterCreate
import kz.mybrain.superkassa.domain.cabinet.model.RetailPlaceAddress
import kz.mybrain.superkassa.domain.cabinet.port.CabinetPorts
import kz.mybrain.superkassa.domain.cabinet.port.Signer
import kz.mybrain.superkassa.integrations.bfdcabinet.BfdCabinet
import kz.mybrain.superkassa.integrations.bfdcabinet.CabinetSettings
import kz.mybrain.superkassa.jsonHttp
import kz.mybrain.superkassa.signedPorts
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Порты кабинета на модуле `bfd-cabinet`: вход, списки и неудачи — видом
 * предметной области.
 *
 * Проверяется то, из-за чего раздел показал бы неверное: итог смены адреса,
 * вошедший по подписи, отделение конца сеанса от отказа по существу
 * и молчания кабинета от его отказа.
 */
class RemoteCabinetTest {

    private fun cabinetReturning(status: HttpStatusCode, body: String): CabinetPorts {
        val engine = MockEngine { respond(body, status, headersOf(HttpHeaders.ContentType, "application/json")) }
        return CabinetWire(http = jsonHttp(engine)).signedPorts()
    }

    /**
     * Смена адреса отвечает результатом проверки, а не самой точкой — HTTP 200
     * в обоих исходах. Приложение ждало здесь точку, и удавшаяся смена адреса
     * падала на разборе: владелец читал «Кабинет не отвечает» о смене,
     * которая состоялась.
     */
    @Test
    fun `смена адреса доходит в оба исхода`() {
        val done = runBlocking {
            cabinetReturning(
                HttpStatusCode.OK,
                """{"retailPlaceId":"b9da2db3","updated":true,"changeMode":"DIRECT","blockingCashRegisters":[]}"""
            ).places.move("place-1", ADDRESS)
        }
        assertTrue(done.updated, "удавшаяся смена адреса прочитана отказом")
        assertFalse(done.needsReregistration)

        val stayed = runBlocking {
            cabinetReturning(
                HttpStatusCode.OK,
                """{"retailPlaceId":"b9da2db3","updated":false,"changeMode":"REREGISTRATION_REQUIRED",
                   "blockingCashRegisters":[{"id":"1","internalName":"Касса проверки","registrationNumber":"260940000031"},
                   {"id":"2","registrationNumber":"260940000026"}]}"""
            ).places.move("place-1", ADDRESS)
        }
        assertTrue(stayed.needsReregistration, "отказ по состоянию касс принят за успех")
        assertEquals(listOf("Касса проверки", "260940000026"), stayed.blockingCashRegisters.map { it.title() })
    }

    /** Задача входа уходит подписывающему как есть, вошедший приходит с компанией. */
    @Test
    fun `вход по подписи называет вошедшего`() = runBlocking {
        val signed = mutableListOf<String>()
        val signer = object : Signer {
            override suspend fun sign(payload: String): String = "cms".also { signed += payload }
        }
        val engine = MockEngine { request ->
            val body = if (request.url.encodedPath == CHALLENGE) TASK else LOGIN
            respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val bfd = BfdCabinet(CabinetSettings(), signing(signer), engine = engine)
        val cabinet = RemoteCabinet(bfd, signer, KeptFiles())

        cabinet.account.signIn()

        assertEquals(listOf("cGF5bG9hZA=="), signed)
        assertEquals("230140000000", cabinet.account.owner.first()?.company?.bin)
    }

    @Test
    fun `кассы списка доходят с точкой`() {
        val all = runBlocking {
            cabinetReturning(
                HttpStatusCode.OK,
                """{"page":0,"size":50,"totalElements":1,"items":[{"id":"22222222","kkmId":2000302,"status":"REGISTERED",
                   "registrationNumber":"260940000031","model":{"modelCode":"M1","name":"Суперкасса"},
                   "retailPlace":{"id":"33333333","name":"Магазин у дома"}}]}"""
            ).registers.all()
        }
        assertEquals("260940000031", all.single().registrationNumber)
        assertEquals("Магазин у дома", all.single().retailPlace?.name)
    }

    /**
     * Заведённая касса встаёт под свою точку, даже если кабинет не назвал
     * точку в ответе: без неё касса не появлялась в списке точек до нового
     * входа.
     */
    @Test
    fun `заведённая касса стоит под выбранной точкой`() {
        val added = runBlocking {
            cabinetReturning(
                HttpStatusCode.OK,
                """{"id":"44444444","kkmId":2000303,"status":"DRAFT","internalName":"Демо 1"}"""
            ).registers.add(RegisterCreate("33333333", "M1", "SN-1", 2026, "Демо 1"))
        }
        assertEquals("33333333", added.retailPlace?.id, "касса без точки не встанет ни под одну точку")
    }

    /** 401 при выданном доступе — конец сеанса: владельца возвращают ко входу, а не показывают отказ. */
    @Test
    fun `истёкший доступ отличается от отказа по существу`() = runBlocking {
        val cabinet = cabinetReturning(HttpStatusCode.Unauthorized, """{"code":"UNAUTHORIZED","message":"Истекла"}""")

        assertFailsWith<CabinetExpired> { cabinet.registers.one("x") }
        assertNull(cabinet.account.owner.first(), "вошедший остался после конца сеанса")
    }

    @Test
    fun `отказ без кода назван состоянием ответа`() {
        val broken = cabinetReturning(HttpStatusCode.BadGateway, "<html>gateway</html>")
        val refusal = assertFailsWith<CabinetRefusal> { runBlocking { broken.company.company() } }
        assertEquals("HTTP_502", refusal.code)
        assertEquals(HttpStatusCode.BadGateway.value, refusal.httpStatus)
    }

    /** Молчание кабинета — не отказ: владельцу говорят проверить связь, а не исправить ввод. */
    @Test
    fun `молчание кабинета названо молчанием`() {
        val silent = CabinetWire(baseUrl = "http://127.0.0.1:1").signedPorts()
        val failure = assertFailsWith<CabinetUnreachable> { runBlocking { silent.places.all() } }
        assertTrue(failure.reason.isNotBlank())
    }

    private companion object {
        val ADDRESS = RetailPlaceAddress(
            addressRef = "0201300118384402",
            latitude = Decimal.parse("43.238949"),
            longitude = Decimal.parse("76.889709")
        )
        const val CHALLENGE = "/api/auth/eds/challenge"
        const val TASK = """{"challengeId":"c-1","payload":"cGF5bG9hZA=="}"""
        const val LOGIN = """{"accessToken":"a-1","user":{"id":"u","iin":"900101300000","fullName":"Курманов Азамат"},
            "company":{"id":"c","bin":"230140000000","name":"ТОО Азик и Ко"}}"""
    }
}
