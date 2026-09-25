package kz.mybrain.superkassa.presentation.cabinet.places

import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kz.mybrain.superkassa.CabinetRig
import kz.mybrain.superkassa.CabinetWire
import kz.mybrain.superkassa.domain.cabinet.model.RegisterAddress
import kz.mybrain.superkassa.jsonHttp
import kz.mybrain.superkassa.kassa.inlineMain
import kz.mybrain.superkassa.presentation.common.mapview.MapPoint
import kz.mybrain.superkassa.presentation.common.message.Message
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Заведение и переезд точки — так, как отвечает кабинет ECC.
 *
 * Тела ответов — по схеме кабинета: на точку с тем же адресом и местом
 * он отвечает `201` и уже заведённой точкой, а кассы, мешающие переезду,
 * называет только идентификатором и состоянием.
 */
class PlacesViewModelTest {

    private val texts = textsOf(Language.Ru).cabinet

    @Test
    fun `точка по занятому адресу и месту не выдаётся за новую`(): Unit = inlineMain {
        val rig = rig { method, path ->
            when {
                method == HttpMethod.Post && path == PLACES -> EXISTING
                path == PLACES -> """{"page":0,"size":50,"totalElements":1,"items":[$EXISTING]}"""
                else -> EMPTY
            }
        }
        val model = PlacesViewModel(rig.model)
        var closed = false

        model.add("Новая точка", ADDRESS, POINT) { closed = true }
        val said = rig.said()

        assertFalse(closed, "окно закрылось, как будто точка заведена")
        assertTrue(said.contains(texts.placeExists), "владельцу не сказано, что новой точки нет: $said")
        assertTrue(said.contains("Магазин на Абая"), "не названа точка, которая уже стоит по адресу: $said")
    }

    @Test
    fun `переезду мешающие кассы названы по списку касс`(): Unit = inlineMain {
        val rig = rig { method, path ->
            when {
                method == HttpMethod.Put -> BLOCKED
                path == PLACES -> """{"page":0,"size":50,"totalElements":1,"items":[$EXISTING]}"""
                path == REGISTERS -> """{"page":0,"size":50,"totalElements":1,"items":[$REGISTER]}"""
                else -> EMPTY
            }
        }
        val model = PlacesViewModel(rig.model)
        val place = rig.model.state.value.places.single()

        model.move(place, ADDRESS, POINT) {}
        val said = rig.said()

        assertTrue(said.contains(texts.addressNeedsReregistration), "не сказано, почему адрес прежний: $said")
        assertTrue(said.contains("Касса у входа"), "не названа касса, мешающая переезду: $said")
    }

    /** Вошедший кабинет с ответами [answer]; ждёт, пока прочитаны точки и кассы. */
    private fun rig(answer: (HttpMethod, String) -> String): CabinetRig {
        val engine = MockEngine { request ->
            val body = answer(request.method, request.url.encodedPath)
            respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val rig = CabinetRig(CabinetWire(http = jsonHttp(engine))).enter()
        waitFor { rig.model.state.value.placesRead && !rig.model.state.value.busy }
        return rig
    }

    /** Что сказано владельцу строкой сообщений, когда кабинет ответил. */
    private fun CabinetRig.said(): String {
        waitFor { services.talk.notices.last is Message.Refusal }
        return (services.talk.notices.last as? Message.Refusal)?.text.orEmpty()
    }

    private fun waitFor(done: () -> Boolean) {
        val until = System.nanoTime() + WAIT_NANOS
        while (!done() && System.nanoTime() < until) Thread.sleep(STEP_MILLIS)
    }

    private companion object {
        const val WAIT_NANOS = 5_000_000_000L
        const val STEP_MILLIS = 10L
        const val PLACES = "/api/retail-places"
        const val REGISTERS = "/api/cash-registers"
        const val EMPTY = """{"page":0,"size":50,"totalElements":0,"items":[]}"""

        const val EXISTING = """{"id":"p-1","name":"Магазин на Абая","addressRef":"0202247079279855",
            "rka":"0202247079279855","cato":"751110000","address":"Алматы, Алмалинский, Абая, 1",
            "latitude":43.238949,"longitude":76.889709,"cashRegisterCount":1}"""

        const val REGISTER = """{"id":"r-9","kkmId":5000009,"internalName":"Касса у входа","status":"REGISTERED",
            "registrationNumber":"000000010009","factoryNumber":"SN-9","modelName":"«ПОРТ FPG-350 ФKZ»",
            "retailPlaceId":"p-1","retailPlaceName":"Магазин на Абая"}"""

        /** Ответ кабинета ECC на переезд точки с кассой на учёте. */
        const val BLOCKED = """{"retailPlaceId":"p-1","updated":false,"changeMode":"REREGISTRATION_REQUIRED",
            "affectedDraftCashRegisters":[],"blockingCashRegisters":[{"id":"r-9","status":"REGISTERED"}]}"""

        val ADDRESS = RegisterAddress(addressRef = "0202247079279855", address = "Алматы, Алмалинский, Абая, 1")
        val POINT = MapPoint(Decimal.parse("43.238949"), Decimal.parse("76.889709"))
    }
}
