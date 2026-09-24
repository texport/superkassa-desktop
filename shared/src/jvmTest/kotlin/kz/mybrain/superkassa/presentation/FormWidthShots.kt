package kz.mybrain.superkassa.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import io.github.texport.superkassa.core.presentation.api.model.reference.OfdEnvironmentResponse
import io.github.texport.superkassa.core.presentation.api.model.reference.TrilingualMessageResponse
import kz.mybrain.superkassa.CabinetStage
import kz.mybrain.superkassa.KassaScene
import kz.mybrain.superkassa.ProbeNode
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.SettingsMeasure
import kz.mybrain.superkassa.StubReply
import kz.mybrain.superkassa.idleCabinet
import kz.mybrain.superkassa.presentation.cabinet.CabinetUiState
import kz.mybrain.superkassa.presentation.cabinet.CabinetWindow
import kz.mybrain.superkassa.presentation.cabinet.company.CompanyScreen
import kz.mybrain.superkassa.presentation.settings.SettingsScene
import kz.mybrain.superkassa.presentation.setup.SetupActions
import kz.mybrain.superkassa.presentation.setup.SetupContent
import kz.mybrain.superkassa.presentation.setup.SetupParts
import kz.mybrain.superkassa.presentation.setup.SetupUiState
import kz.mybrain.superkassa.presentation.setup.registration.RegistrationActions
import kz.mybrain.superkassa.presentation.setup.registration.RegistrationUiState
import kz.mybrain.superkassa.presentation.shell.ProvideWindowModels
import kz.mybrain.superkassa.presentation.shell.WindowModels
import kz.mybrain.superkassa.presentation.shell.frame.WindowParts
import kz.mybrain.superkassa.presentation.shell.rail.SectionRail
import kz.mybrain.superkassa.presentation.shell.section.Section
import kz.mybrain.superkassa.presentation.shell.section.SectionContent
import kz.mybrain.superkassa.presentation.shell.section.sectionFrame
import kz.mybrain.superkassa.presentation.theme.size.Spacing
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kz.mybrain.superkassa.tap
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Формы и карточки во всю доступную ширину раздела.
 *
 * Настройки, деньги, кассиры, очередь, мастер подключения и страница
 * компании стояли колонкой шириной чтения, и на широком окне правая
 * половина экрана пустовала — владелец принимал это за поломку. Меряется
 * правый край содержимого раздела против правого края окна;
 * кадры — `/tmp/width-<экран>-<окно>.png`.
 */
class FormWidthShots {

    /** Экран раздела; у настроек две вкладки, и вторая открывается нажатием. */
    private enum class Screen(val section: Section, val tab: ((Language) -> String)? = null) {
        SettingsKkm(Section.Settings),
        SettingsWorkplace(Section.Settings, { textsOf(it).common.settings.householdWorkplace }),
        Cash(Section.Cash),
        Users(Section.Users),
        Queue(Section.Queue),
        Register(Section.Register)
    }

    /** Правый край рельса разделов в последнем кадре: левее начинается не раздел. */
    private var rail = 0f

    /** Рельс разделов, как в окне кассы, и раздел правее него. */
    @Composable
    private fun WithRail(section: Section, content: @Composable () -> Unit) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(Modifier.onGloballyPositioned { rail = it.positionInRoot().x + it.size.width }) {
                SectionRail(Section.entries, section, false, {}, { Text(VERSION) }) {}
            }
            content()
        }
    }

    @Composable
    private fun Window(screen: Screen) {
        val desk = remember { KassaScene.desk(SettingsMeasure.extremeKkm(), admin = true) }
        val app = remember { SettingsScene.app(desk) }
        val parts = remember { WindowParts(desk.parts.shell, desk.look, idleCabinet(app, desk.look)) }
        Surface(Modifier.fillMaxSize()) {
            WithRail(screen.section) {
                if (screen == Screen.Register) {
                    Wizard(checkNotNull(parts.cabinet))
                } else {
                    ProvideWindowModels(remember { WindowModels() }) { SectionContent(app, parts, screen.section) }
                }
            }
        }
    }

    /**
     * Мастер на первом шаге, в пределах рабочего экрана, как его ставит
     * каркас: раздел целиком требует портов мастера, которых у сцены нет.
     */
    @Composable
    private fun Wizard(cabinet: CabinetWindow) {
        val contours = listOf("TEST", "PROD").map { OfdEnvironmentResponse(it, TrilingualMessageResponse(it, it, it)) }
        Box(modifier = Modifier.sectionFrame()) {
            SetupContent(
                SetupParts(
                    state = SetupUiState(contours = contours),
                    actions = object : SetupActions {},
                    registration = RegistrationUiState(),
                    registrationActions = object : RegistrationActions {},
                    cabinet = cabinet,
                    window = CabinetUiState(),
                    onBack = null
                )
            )
        }
    }

    /**
     * Правый край содержимого раздела: самый правый узел с надписью, полем
     * или переключателем правее рельса.
     */
    private fun contentRight(nodes: List<ProbeNode>): Float =
        nodes.filter { it.at.x > rail && it.visible.width > 0f && (it.text.isNotBlank() || it.role != null) }
            .maxOf { it.visible.right }

    /** Правый край раздела-формы: всё окно правее рельса. */
    private fun workspaceRight(width: Int): Float = width.toFloat()

    private fun measure(name: String, width: Int, probe: RenderProbe): String? {
        val right = contentRight(probe.nodes())
        val edge = workspaceRight(width)
        val filled = (right - rail) / (edge - rail)
        println("ширина $name: содержимое до $right, рабочее место до $edge, занято ${(filled * PERCENT).toInt()}%")
        return "$name: занято ${(filled * PERCENT).toInt()}% ширины раздела".takeIf { filled < FILLED }
    }

    private fun shoot(screen: Screen, width: Int, height: Int): String? {
        val name = "${screen.name.lowercase()}-${width}x$height"
        return RenderProbe(width, height) { Window(screen) }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            screen.tab?.let { tab -> probe.tap { it.text == tab(Language.Ru) } }
            repeat(SETTLE) { probe.frame() }
            File("/tmp/width-$name.png").writeBytes(probe.frame())
            measure(name, width, probe)
        }
    }

    /** Страница компании: реквизиты и три вида деятельности. */
    private fun company(width: Int, height: Int): String? {
        val stage = CabinetStage { path -> StubReply(if (path == "/api/company") COMPANY else "{}") }
        val name = "company-${width}x$height"
        return RenderProbe(width, height) {
            Surface(Modifier.fillMaxSize()) {
                WithRail(Section.Cabinet) {
                    stage.Window {
                        Column(modifier = Modifier.fillMaxWidth().padding(Spacing.fieldGap)) {
                            CompanyScreen(stage.cabinet.cabinet, Language.Ru, stage.texts)
                        }
                    }
                }
            }
        }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            stage.settled()
            repeat(SETTLE) { probe.frame() }
            File("/tmp/width-$name.png").writeBytes(probe.frame())
            measure(name, width, probe)
        }
    }

    @Test
    fun `на широком окне формы и карточки занимают ширину раздела`() {
        val gaps = SIZES.flatMap { (width, height) ->
            Screen.entries.map { shoot(it, width, height) } + company(width, height)
        }.filterNotNull()
        assertTrue(gaps.isEmpty(), gaps.joinToString("\n"))
    }

    private companion object {
        /** Настольные 1920×1080 и 2560×1080, планшеты лёжа 1280×800 и 2560×1600. */
        val SIZES = listOf(1920 to 1080, 2560 to 1080, 1280 to 800, 2560 to 1600)
        const val VERSION = "1.0.6"
        const val SETTLE = 20
        const val PERCENT = 100

        /** Меньше этой доли — справа пустая полоса; узел с надписью стоит левее края карточки. */
        const val FILLED = 0.8f

        const val COMPANY = """{"id":"c-1","bin":"230140000000","name":"ТОО «Азик и Ко»","okeds":[
            {"code":"47.11","name":"Розничная торговля продуктами питания","primary":true},
            {"code":"56.10","name":"Деятельность ресторанов и предоставление услуг по доставке","primary":false}]}"""
    }
}
