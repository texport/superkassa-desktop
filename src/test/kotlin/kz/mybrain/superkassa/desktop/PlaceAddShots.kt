package kz.mybrain.superkassa.desktop

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetClient
import kz.mybrain.superkassa.desktop.ui.cabinet.AddPlaceCard
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Снимки окна заведения торговой точки.
 *
 * Все три отказных случая здесь про регистр: он ответил пусто, он не
 * ответил вовсе и он ответил, но выбранного дома владелец ещё не дошёл.
 * Различать их на экране обязательно: «адреса нет в регистре» и «регистр
 * молчит» чинятся по-разному.
 */
class PlaceAddShots {

    /** Пустая форма: ни названия, ни адреса, ни места. */
    @Test
    fun `пустая форма`() = look("place-add-empty", registry(EMPTY))

    /** Регистр ответил пусто: такого адреса в нём нет. */
    @Test
    fun `адрес не найден в регистре`() = look("place-add-not-found", registry(EMPTY), typed = true)

    /** Регистр не ответил вовсе: это не то же, что «адреса нет». */
    @Test
    fun `регистр не ответил`() =
        look("place-add-registry-silent", registry(EMPTY, HttpStatusCode.BadGateway), typed = true)

    /** Регистр ответил областями: с них начинается подбор адреса. */
    @Test
    fun `регистр ответил областями`() = look("place-add-regions", registry(REGIONS))

    /**
     * Снимок окна заведения точки.
     *
     * Владелец входит отметкой разработчика: без доступа окно не спросило
     * бы регистр вовсе, и отказных состояний на снимке не было бы.
     */
    private fun look(name: String, cabinet: CabinetSession, typed: Boolean = false) {
        val session = Look.session()
        RenderProbe(WIDE, HIGH) { AddPlaceCard(session, cabinet, Look.cabinet, onDismiss = {}, onAdded = {}) }
            .use { probe ->
                repeat(SETTLE) { probe.frame() }
                // Набранное в поле шага: регистр спрашивается за набором,
                // и без него отказных ответов на экране не бывает.
                if (typed) {
                    probe.click(STEP_FIELD)
                    probe.key(Key.A)
                    repeat(SETTLE) { probe.frame() }
                }
                Look.shot(name, probe.frame())
            }
    }

    /** Кабинет, отвечающий за регистр заданным телом. */
    private fun registry(body: String, status: HttpStatusCode = HttpStatusCode.OK): CabinetSession {
        val engine = MockEngine { request ->
            val me = request.url.encodedPath.endsWith("/me")
            respond(
                content = if (me) WHO else body,
                status = if (me) HttpStatusCode.OK else status,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val http = HttpClient(engine) {
            expectSuccess = false
            install(ContentNegotiation) { json(CabinetClient.lenientJson) }
        }
        val cabinet = CabinetSession(CabinetClient(http = http))
        assertTrue(runBlocking { cabinet.signInAsDeveloper(IIN, BIN) }, "владелец не вошёл")
        return cabinet
    }

    private companion object {
        /** Кто вошёл: ИИН владельца и БИН его компании. */
        const val IIN = "870101300123"
        const val BIN = "180140000123"

        val WHO = """{"user":{"id":"u-1","iin":"$IIN","fullName":"Иванов Сергей"},
            "company":{"id":"c-1","bin":"$BIN","name":"ТОО «Пример»"}}"""

        const val EMPTY = """{"items":[]}"""

        const val REGIONS = """{"items":[
            {"id":1,"name":"Алматы","level":"REGION"},
            {"id":2,"name":"Астана","level":"REGION"},
            {"id":3,"name":"Карагандинская область","level":"REGION"}]}"""

        /** Середина поля текущего шага подбора: по нему набирают запрос. */
        val STEP_FIELD = Offset(590f, 430f)

        const val SETTLE = 24
        const val WIDE = 1180
        const val HIGH = 820
    }
}
