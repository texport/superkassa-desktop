package kz.mybrain.superkassa.desktop

import kotlinx.coroutines.runBlocking
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetRegister
import kz.mybrain.superkassa.desktop.ui.cabinet.RegistrationActionsBlock
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Во что обходится открытие карточки кассы.
 *
 * Точки компании читает раздел, и лежат они в сеансе: заявление о
 * перерегистрации выбирает точку из того же списка. Карточка кассы
 * спрашивала их заново при каждом открытии — у сети это двадцать
 * страниц на каждое нажатие по строке списка, и за ними владелец ждёт
 * открытия карточки, ради которой нажал.
 */
class CabinetRegisterCardReadsTest {

    private val asked = mutableListOf<String>()

    private fun stage() = CabinetStage { path ->
        asked += path
        when (path) {
            "/api/retail-places" -> CabinetReply(page(PLACES))
            else -> CabinetReply("{}")
        }
    }

    private fun page(total: Int) = """{"page":0,"size":50,"totalElements":$total,"items":[${rows(total)}]}"""

    private fun rows(total: Int) =
        (0 until minOf(total, PAGE)).joinToString(",") { """{"id":"p-$it","name":"Торговая точка $it"}""" }

    private fun register() = CabinetRegister(id = "r-1", kkmId = 5_000_021, status = "DRAFT")

    @Test
    fun `открытая карточка кассы не перечитывает точки, уже прочитанные разделом`() {
        val stage = stage()
        runBlocking { stage.cabinet.refreshPlaces() }
        asked.clear()

        RenderProbe(WIDE, HIGH) {
            RegistrationActionsBlock(stage.session, stage.cabinet, stage.texts, register()) {}
        }.use { probe -> repeat(SETTLE) { probe.frame() } }

        assertEquals(
            emptyList(),
            asked.filter { it == "/api/retail-places" },
            "карточка кассы сходила за списком точек, который уже прочитан"
        )
    }

    private companion object {
        const val PAGE = 50
        const val PLACES = 200
        const val WIDE = 700
        const val HIGH = 500
        const val SETTLE = 30
    }
}
