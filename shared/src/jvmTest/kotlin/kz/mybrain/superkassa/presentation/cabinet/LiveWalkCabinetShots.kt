package kz.mybrain.superkassa.presentation.cabinet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import kz.mybrain.superkassa.CabinetStage
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.StubReply
import kz.mybrain.superkassa.data.cabinet.CabinetBodies
import kz.mybrain.superkassa.designsystem.section.CollapsibleCard
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.domain.cabinet.model.CompanyProfile
import kz.mybrain.superkassa.domain.cabinet.model.OKED_PAGE
import kz.mybrain.superkassa.domain.cabinet.model.Oked
import kz.mybrain.superkassa.presentation.cabinet.applications.RegistrationActionsBlock
import kz.mybrain.superkassa.presentation.cabinet.company.AddOkedCard
import kz.mybrain.superkassa.presentation.cabinet.company.CompanyActions
import kz.mybrain.superkassa.presentation.cabinet.company.CompanyUiState
import kz.mybrain.superkassa.presentation.cabinet.company.OkedsCard
import kz.mybrain.superkassa.presentation.cabinet.company.actions
import kz.mybrain.superkassa.presentation.cabinet.company.companyViewModel
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kz.mybrain.superkassa.viewOf
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Снимки того, что владелец увидел на живом проходе кабинета и мастера.
 *
 * Здесь смотрят на доступность действий и на подсказки: залитая кнопка
 * над невыбранной точкой, «Сохранить» на нетронутой карточке и подсказка
 * о длине классификатора до единого набранного знака видны только глазами.
 */
class LiveWalkCabinetShots {

    private fun stage() = CabinetStage { path ->
        when {
            path == "/api/retail-places" -> StubReply(CabinetBodies.PLACES)
            path == "/api/reference/okeds" -> StubReply(okedPage())
            path.startsWith("/api/cash-registers/") -> StubReply(DRAFT_REGISTER)
            else -> StubReply("{}")
        }
    }

    /**
     * Полная страница классификатора при двух тысячах позиций всего.
     *
     * Иначе подсказка «классификатор длиннее показанного» не выходит
     * вовсе: у короткого ответа уточнять нечего, и снимок показал бы
     * не то состояние, в котором владелец её увидел.
     */
    private fun okedPage(): String = (1..OKED_PAGE).joinToString(",") { at ->
        """{"code":"47.%02d","name":"Розничная торговля, вид $at"}""".format(at)
    }.let { items -> """{"items":[$items],"total":$OKED_TOTAL}""" }

    /** Перерегистрация без выбранной точки: подача недоступна и сказано почему. */
    @Test
    fun `подача без выбранной точки недоступна`() {
        val stage = stage()
        val shot = fix("5-place-not-chosen", CARD, TALL) {
            stage.Window {
                Column(Modifier.fillMaxWidth().padding(Spacing.fieldGap), Arrangement.spacedBy(Spacing.fieldGap)) {
                    RegistrationActionsBlock(stage.cabinet.cabinet, Language.Ru, stage.texts, viewOf(onRecord())) {}
                }
            }
        }
        assertTrue(shot.isNotEmpty())
    }

    /** Нетронутая карточка видов деятельности: сохранять нечего. */
    @Test
    fun `сохранение видов деятельности ждёт правки`() {
        val kept = fix("6-okeds-unchanged", CARD, TALL) { Okeds(changed = false) }
        val edited = fix("6-okeds-changed", CARD, TALL) { Okeds(changed = true) }
        assertTrue(!kept.contentEquals(edited), "нетронутая и правленая карточки обязаны различаться")
    }

    /**
     * Классификатор ОКЭД до запроса: обычная подсказка, а не «уточните запрос».
     *
     * Карточка заведения свёрнута, и раскрывает её нажатие — то же, каким
     * её раскрывает владелец: состояние «только что раскрыли, ничего
     * не набирали» иначе не воспроизвести.
     */
    @Test
    fun `подсказка классификатора ждёт запроса`() {
        val stage = stage()
        RenderProbe(CARD, TALL) {
            stage.Window {
                val model = companyViewModel(stage.cabinet.cabinet)
                val state by model.state.collectAsState()
                Column(modifier = Modifier.fillMaxWidth().padding(Spacing.fieldGap)) {
                    AddOkedCard(state.search, Language.Ru, stage.texts, model.actions())
                }
            }
        }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            probe.click(Offset(EXPAND_X, EXPAND_Y))
            repeat(SETTLE) { probe.frame() }
            val frame = probe.frame()
            File("/tmp/fix-7-oked-hint.png").writeBytes(frame)
            assertTrue(frame.isNotEmpty())
        }
    }

    /** Свёрнутые разделы карточки кассы: у каждого значок объяснения. */
    @Test
    fun `у карты и журнала есть подсказка`() {
        val texts = stage().texts
        val shot = fix("8-section-hints", CARD, CHIPS) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(Spacing.fieldGap),
                verticalArrangement = Arrangement.spacedBy(Spacing.fieldGap)
            ) {
                CollapsibleCard(texts.card, expanded = false, onToggle = {}, info = texts.hints.card) {}
                val journal = texts.actionsJournal
                CollapsibleCard(journal, expanded = false, onToggle = {}, info = texts.hints.actionsJournal) {}
            }
        }
        assertTrue(shot.isNotEmpty())
    }

    @Composable
    private fun Okeds(changed: Boolean) {
        val texts = textsOf(Language.Ru).cabinet
        val saved = listOf(Oked(code = "47.11", name = "Розничная торговля", primary = true))
        val added = Oked(code = "56.10", name = "Рестораны и услуги по доставке еды")
        val profile = CompanyProfile(id = "c-1", bin = "230140000000", name = "ТОО «Азик и Ко»", okeds = saved)
        val state = CompanyUiState(profile = profile, okeds = if (changed) saved + added else saved)
        Column(modifier = Modifier.fillMaxWidth().padding(Spacing.fieldGap)) {
            OkedsCard(texts, state, busy = false, title = { it.name.orEmpty() }, actions = object : CompanyActions {})
        }
    }

    /** Касса на учёте: перерегистрация — то заявление, которому нужна точка. */
    private fun onRecord() =
        CabinetRegister(id = "r-1", kkmId = 5_000_021, status = "REGISTERED", registrationNumber = "000000010001")

    /** Снимок под тем же именем, что и пункт списка владельца. */
    private fun fix(name: String, width: Int, height: Int, content: @Composable () -> Unit): ByteArray =
        RenderProbe(width = width, height = height, content = content).use { probe ->
            var frame = probe.frame()
            repeat(SETTLE) { frame = probe.frame() }
            File("/tmp/fix-$name.png").writeBytes(frame)
            frame
        }

    private companion object {
        const val CARD = 720
        const val TALL = 620
        const val CHIPS = 220
        const val SETTLE = 60

        /** Стрелка, раскрывающая карточку заведения вида деятельности. */
        const val EXPAND_X = 668f
        const val EXPAND_Y = 52f

        /** Всего позиций в классификаторе: столько отдаёт кабинет. */
        const val OKED_TOTAL = 2107

        /** Касса, которую кабинет ещё не поставил на учёт: состояние приходит кодом. */
        const val DRAFT_REGISTER = """{"id":"r-1","kkmId":5000021,"status":"DRAFT",
            "internalName":"Касса у входа","factoryNumber":"SN-ECC-172758"}"""
    }
}
