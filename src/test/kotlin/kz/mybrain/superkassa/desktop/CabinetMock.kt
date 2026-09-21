package kz.mybrain.superkassa.desktop

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetClient
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetCompany
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetLogin
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetUser

/**
 * Кабинет, отвечающий заданным, — для снимков отказных состояний.
 *
 * Отказные состояния собираются только настоящим сеансом с доступом:
 * без него экраны не спрашивают кабинет вовсе и остаются в ожидании,
 * а проверять нужно как раз ответ. Вход идёт отметкой разработчика —
 * одним запросом, — а дальше все ручки отвечают одинаково.
 *
 * @param body тело ответа на любой запрос, кроме входа.
 * @param status состояние ответа: `404` означает невыложенный раздел,
 *   прочие отказы — отказ кабинета по существу.
 */
internal fun mockCabinet(body: String, status: HttpStatusCode = HttpStatusCode.OK): CabinetSession {
    val engine = MockEngine { request ->
        val entering = request.url.encodedPath.endsWith("/me")
        respond(
            content = if (entering) WHO else body,
            status = if (entering) HttpStatusCode.OK else status,
            headers = headersOf(HttpHeaders.ContentType, "application/json")
        )
    }
    val http = HttpClient(engine) {
        expectSuccess = false
        install(ContentNegotiation) { json(CabinetClient.lenientJson) }
    }
    val cabinet = CabinetSession(CabinetClient(http = http))
    // Вход по ЭЦП требует NCALayer, которого в проверке нет: сеанс ставится
    // тем же доступом, каким его поставил бы ответ кабинета на вход.
    cabinet.access.enter(entered())
    return cabinet
}

/** Доступ и вошедший — то, что кабинет отдаёт на успешный вход по ЭЦП. */
private fun entered() = CabinetLogin(
    accessToken = "test-access",
    user = CabinetUser(id = "u-1", iin = IIN, fullName = "Иванов Сергей"),
    company = CabinetCompany(id = "c-1", bin = BIN, name = "ТОО «Пример»")
)

/** ИИН владельца и БИН его компании: подставные, но казахстанского вида. */
private const val IIN = "870101300123"
private const val BIN = "180140000123"

private val WHO = """{"user":{"id":"u-1","iin":"$IIN","fullName":"Иванов Сергей"},
    "company":{"id":"c-1","bin":"$BIN","name":"ТОО «Пример»"}}"""
