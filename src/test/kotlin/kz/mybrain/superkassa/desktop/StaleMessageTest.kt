package kz.mybrain.superkassa.desktop

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.respondOk
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetClient
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Устаревшее сообщение не остаётся на экране.
 *
 * Отказ висит, пока владелец его не закроет, — и в мастере подключения
 * «Кабинет не отвечает по заданному адресу» стояло под четвёртым успешно
 * пройденным шагом: связь давно восстановилась, а строка осталась.
 * Отметка об ответе кабинета даёт каркасу право снять своё сообщение.
 */
class StaleMessageTest {

    private fun cabinet(answering: Boolean): CabinetSession {
        val http = HttpClient(
            MockEngine { if (answering) respondOk("{}") else respondError(HttpStatusCode.ServiceUnavailable) }
        )
        return CabinetSession(CabinetClient(http = http))
    }

    @Test
    fun `ответ кабинета отмечается, а отказ нет`() {
        val silent = cabinet(answering = false)
        runBlocking { silent.guard { error("кабинет молчит") } }
        assertEquals(0, silent.answered, "молчание записано ответом")

        val talking = cabinet(answering = true)
        runBlocking { talking.guard { true } }
        assertEquals(1, talking.answered, "ответ не отмечен, и отказ останется висеть")
    }

    /**
     * Язык рабочего места: своего выбора нет — берётся язык системы.
     *
     * Свежее рабочее место встречало владельца казахским независимо
     * от машины. Государственный язык остаётся для системы, языка которой
     * касса не знает.
     */
    @Test
    fun `язык без выбора берётся у системы`() {
        assertEquals(Language.Ru, Language.byCode(code = null, system = "ru"))
        assertEquals(Language.Kk, Language.byCode(code = null, system = "fr"))
        assertEquals(Language.Kk, Language.byCode(code = "kk", system = "ru"))
    }
}
