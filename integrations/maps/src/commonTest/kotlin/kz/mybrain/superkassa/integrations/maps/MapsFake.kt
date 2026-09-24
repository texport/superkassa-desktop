package kz.mybrain.superkassa.integrations.maps

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf

/**
 * Подставные службы карты: отвечают заданным телом и запоминают запросы.
 *
 * По запомненным запросам видно, ушёл ли вопрос в сеть вовсе, с каким
 * именем приложения и с какими параметрами.
 */
internal class MapsFake(
    private val answer: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData
) {
    val asked = mutableListOf<HttpRequestData>()

    val engine = MockEngine { request ->
        asked += request
        answer(request)
    }

    companion object {
        fun json(body: String): MapsFake = MapsFake { respond(body, HttpStatusCode.OK, jsonHeaders) }

        val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
    }
}
