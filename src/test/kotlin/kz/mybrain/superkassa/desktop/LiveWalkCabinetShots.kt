package kz.mybrain.superkassa.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import kz.mybrain.superkassa.desktop.app.KkmSetupDraft
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetRegister
import kz.mybrain.superkassa.desktop.server.cabinet.Oked
import kz.mybrain.superkassa.desktop.ui.cabinet.AddOkedCard
import kz.mybrain.superkassa.desktop.ui.cabinet.OkedsCard
import kz.mybrain.superkassa.desktop.ui.cabinet.RegistrationActionsBlock
import kz.mybrain.superkassa.desktop.ui.components.CollapsibleCard
import kz.mybrain.superkassa.desktop.ui.setup.ApplicationStepCard
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.setupTexts
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
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

    private val setup = setupTexts(Language.Ru)

    private fun stage() = CabinetStage { path ->
        when {
            path == "/api/retail-places" -> CabinetReply(CabinetBodies.PLACES)
            path == "/api/reference/okeds" -> CabinetReply(okedPage())
            path.startsWith("/api/cash-registers/") -> CabinetReply(DRAFT_REGISTER)
            else -> CabinetReply("{}")
        }
    }

    /**
     * Полная страница классификатора при двух тысячах позиций всего.
     *
     * Иначе подсказка «классификатор длиннее показанного» не выходит
     * вовсе: у короткого ответа уточнять нечего, и снимок показал бы
     * не то состояние, в котором владелец её увидел.
     */
    private fun okedPage(): String {
        val items = (1..OKED_PAGE).joinToString(",") { at ->
            """{"code":"47.%02d","name":"Розничная торговля, вид $at"}""".format(at)
        }
        return """{"items":[$items],"total":$OKED_TOTAL}"""
    }

    /** Перерегистрация без выбранной точки: подача недоступна и сказано почему. */
    @Test
    fun `подача без выбранной точки недоступна`() {
        val shot = fix("5-place-not-chosen", CARD, TALL) {
            val stage = stage()
            Column(
                modifier = Modifier.fillMaxWidth().padding(Spacing.screen),
                verticalArrangement = Arrangement.spacedBy(Spacing.snug)
            ) {
                RegistrationActionsBlock(stage.session, stage.cabinet, stage.texts, onRecord()) {}
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
        RenderProbe(CARD, TALL) {
            val stage = stage()
            Column(modifier = Modifier.fillMaxWidth().padding(Spacing.screen)) {
                AddOkedCard(stage.session, stage.cabinet, stage.texts, remember { mutableStateListOf() })
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
                modifier = Modifier.fillMaxWidth().padding(Spacing.screen),
                verticalArrangement = Arrangement.spacedBy(Spacing.snug)
            ) {
                CollapsibleCard(texts.card, expanded = false, onToggle = {}, info = texts.hints.card) {}
                CollapsibleCard(
                    title = texts.actionsJournal,
                    expanded = false,
                    onToggle = {},
                    info = texts.hints.actionsJournal
                ) {}
            }
        }
        assertTrue(shot.isNotEmpty())
    }

    /** Шаг постановки на учёт: состояние словами и ручное «Обновить» рядом. */
    @Test
    fun `состояние кассы в мастере названо словами`() {
        val shot = fix("13-draft-in-words", CARD, TALL) {
            val stage = stage()
            val draft = remember {
                KkmSetupDraft(stage.session.preferences).apply { rememberRegister("r-1", 5_000_021) }
            }
            Column(modifier = Modifier.fillMaxWidth().padding(Spacing.screen)) {
                ApplicationStepCard(stage.session, stage.cabinet, setup, draft) {}
            }
        }
        assertTrue(shot.isNotEmpty())
    }

    @Composable
    private fun Okeds(changed: Boolean) {
        val stage = stage()
        val saved = listOf(Oked(code = "47.11", name = "Розничная торговля", primary = true))
        val okeds = remember { mutableStateListOf<Oked>().apply { addAll(saved) } }
        if (changed) okeds.add(Oked(code = "56.10", name = "Рестораны и услуги по доставке еды"))
        Column(modifier = Modifier.fillMaxWidth().padding(Spacing.screen)) {
            OkedsCard(stage.texts, okeds, busy = false, saved = saved, title = { it.name.orEmpty() }) {}
        }
    }

    /** Касса на учёте: перерегистрация — то заявление, которому нужна точка. */
    private fun onRecord() = CabinetRegister(
        id = "r-1",
        kkmId = 5_000_021,
        internalName = "Касса у входа",
        status = "REGISTERED",
        registrationNumber = "000000010001",
        factoryNumber = "SN-ECC-172758"
    )

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

        /** Страница классификатора и всего позиций в нём: столько отдаёт кабинет. */
        const val OKED_PAGE = 50
        const val OKED_TOTAL = 2107

        /** Касса, которую кабинет ещё не поставил на учёт: состояние приходит кодом. */
        const val DRAFT_REGISTER = """{"id":"r-1","kkmId":5000021,"status":"DRAFT",
            "internalName":"Касса у входа","factoryNumber":"SN-ECC-172758"}"""
    }
}
