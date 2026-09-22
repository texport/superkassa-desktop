package kz.mybrain.superkassa.desktop

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kz.mybrain.superkassa.desktop.app.CabinetProblem
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetClient
import kz.mybrain.superkassa.desktop.ui.cabinet.cabinetMessage
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.cabinetTexts
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Кабинет ответил, но не тем, чего от него ждали.
 *
 * Так выглядит разошедшийся договор: поле сменило имя или тип, ответ
 * пришёл кодом 200 и не разобрался. Владелец читал об этом «Кабинет
 * не отвечает по заданному адресу» — и шёл проверять сеть и доступ
 * к службе, которая отвечает и работает. Поддержка по тем же словам
 * искала не там, где расхождение.
 */
class CabinetUnreadableTest {

    private val texts = cabinetTexts(Language.Ru)

    @Test
    fun `непонятный ответ не выдаётся за молчание кабинета`() {
        // Поле точки сменило имя: страница на месте, а прочесть точку нечем.
        val cabinet = mockCabinet(
            """{"page":0,"size":50,"totalElements":1,"items":[{"placeId":"p-1","title":"Магазин"}]}""",
            HttpStatusCode.OK
        )

        runBlocking { cabinet.refreshPlaces() }

        val problem = cabinet.problem
        assertTrue(problem is CabinetProblem.Unreadable, "помеха названа так: $problem")
        assertTrue(
            cabinetMessage(problem, texts).words() != texts.unreachable,
            "о разобравшемся не ответе сказано, что кабинет не отвечает"
        )
    }

    /** Молчание остаётся молчанием: подмена одного другим — та же неправда. */
    @Test
    fun `недоступный кабинет остаётся недоступным`() {
        // Порт, на котором никто не слушает: соединение не поднимется.
        val cabinet = mockCabinet(CabinetClient(baseUrl = "http://127.0.0.1:1"))

        runBlocking { cabinet.refreshPlaces() }

        assertEquals(texts.unreachable, cabinet.problem?.let { cabinetMessage(it, texts).words() })
    }
}
