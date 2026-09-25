package kz.mybrain.superkassa.presentation.cabinet

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kz.mybrain.superkassa.CabinetWire
import kz.mybrain.superkassa.replying
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
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

    private val texts = textsOf(Language.Ru).cabinet

    @Test
    fun `непонятный ответ не выдаётся за молчание кабинета`() {
        // Поле точки сменило имя: страница на месте, а прочесть точку нечем.
        val rig = CabinetListsRig(
            CabinetWire(
                http = replying(
                    """{"page":0,"size":50,"totalElements":1,"items":[{"placeId":"p-1","title":"Магазин"}]}""",
                    HttpStatusCode.OK
                )
            )
        )

        runBlocking { rig.lists.readPlaces() }

        val said = rig.state.placesTrouble
        assertEquals(cabinetMessage(CabinetProblem.Unreadable, texts).words(), said, "помеха названа так: $said")
        assertTrue(said != texts.unreachable, "о разобравшемся не ответе сказано, что кабинет не отвечает")
    }

    /** Молчание остаётся молчанием: подмена одного другим — та же неправда. */
    @Test
    fun `недоступный кабинет остаётся недоступным`() {
        // Порт, на котором никто не слушает: соединение не поднимется.
        val rig = CabinetListsRig(CabinetWire(baseUrl = "http://127.0.0.1:1"))

        runBlocking { rig.lists.readPlaces() }

        assertEquals(texts.unreachable, rig.state.placesTrouble)
    }
}
