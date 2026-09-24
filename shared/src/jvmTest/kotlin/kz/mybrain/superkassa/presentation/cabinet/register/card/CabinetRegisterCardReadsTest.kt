package kz.mybrain.superkassa.presentation.cabinet.register.card

import kotlinx.coroutines.runBlocking
import kz.mybrain.superkassa.CabinetStage
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.StubReply
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.presentation.cabinet.applications.RegistrationActionsBlock
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.viewOf
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Во что обходится открытие карточки кассы.
 *
 * Точки компании читает кабинет окна, и лежат они в его модели: заявление о
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
            "/api/retail-places" -> StubReply(page(PLACES))
            else -> StubReply("{}")
        }
    }

    private fun page(total: Int) = """{"page":0,"size":50,"totalElements":$total,"items":[${rows(total)}]}"""

    private fun rows(total: Int) =
        (0 until minOf(total, PAGE)).joinToString(",") { """{"id":"p-$it","name":"Торговая точка $it"}""" }

    private fun register() = CabinetRegister(id = "r-1", kkmId = 5_000_021, status = "DRAFT")

    @Test
    fun `открытая карточка кассы не перечитывает точки, уже прочитанные разделом`() {
        val stage = stage()
        stage.settled()
        runBlocking { stage.cabinet.cabinet.readPlaces() }
        asked.clear()

        RenderProbe(WIDE, HIGH) {
            stage.Window {
                RegistrationActionsBlock(stage.cabinet.cabinet, Language.Ru, stage.texts, viewOf(register())) {}
            }
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
