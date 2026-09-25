package kz.mybrain.superkassa.presentation.cabinet.places

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.ktor.http.HttpStatusCode
import kz.mybrain.superkassa.CabinetStage
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.StubReply
import kz.mybrain.superkassa.app
import kz.mybrain.superkassa.data.cabinet.CabinetPages
import kz.mybrain.superkassa.designsystem.section.AppTopBar
import kz.mybrain.superkassa.designsystem.theme.Look
import kz.mybrain.superkassa.designsystem.theme.TextScale
import kz.mybrain.superkassa.domain.cabinet.PlaceLook
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.domain.cabinet.model.RetailPlace
import kz.mybrain.superkassa.presentation.shell.frame.WindowParts
import kz.mybrain.superkassa.presentation.shell.frame.shellModel
import kz.mybrain.superkassa.presentation.shell.rail.SectionRail
import kz.mybrain.superkassa.presentation.shell.section.Section
import kz.mybrain.superkassa.presentation.shell.section.SectionContent
import kz.mybrain.superkassa.refusal
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kz.mybrain.superkassa.tap
import java.io.File

/**
 * Раздел торговых точек в окне кассы: шапка, рельс разделов и кабинет.
 *
 * Хозяйство — как у владельца сети: две тысячи точек с длинными адресами,
 * название в сотню знаков, казахское слово без переносов и точка с двумя
 * сотнями касс. Меряется то, что владелец видит в окне, а не раздел сам
 * по себе: высоту списку отдают шапка окна и вкладки кабинета.
 */
internal class CabinetPlacesScene(
    val width: Int,
    val height: Int,
    val language: Language = Language.Ru,
    val scale: TextScale = TextScale.Normal
) {
    val texts = textsOf(language).cabinet
    private val stage = CabinetStage(::reply)
    private val parts = WindowParts(shellModel(stage.app()), stage.look, stage.cabinet)

    /** Раздел открыт на вкладке точек, список прочитан; кадр — в файл. */
    fun open(name: String, check: (RenderProbe) -> Unit) {
        RenderProbe(width, height, look = Look(textScale = scale), language = language) { Window() }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            probe.tap { it.text == texts.places }
            repeat(LOADING) { if (probe.nodes().none { it.text.contains(PLACES.size.toString()) }) probe.frame() }
            check(probe)
            File("/tmp/adaptive-cabinet-$name-${width}x$height-${language.name.lowercase()}-${scale.code}.png")
                .writeBytes(probe.frame())
        }
    }

    @Composable
    private fun Window() {
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar(title = "Касса 3", subtitle = "ТОО «Азик и Ко»", subtitleKept = "Курманов Азамат") {}
            Row(modifier = Modifier.fillMaxSize()) {
                SectionRail(Section.entries, Section.Cabinet, false, {}, { Text("1.0.6") }) {}
                stage.Window { SectionContent(stage.app(), parts, Section.Cabinet) }
            }
        }
    }

    private fun reply(path: String): StubReply = when (path) {
        "/api/retail-places" -> StubReply(CabinetPages.places(PLACES))
        "/api/cash-registers" -> StubReply(CabinetPages.registers(REGISTERS))
        else -> refusal("NOT_FOUND", "нет в проверке", HttpStatusCode.NotFound)
    }

    companion object {
        const val SETTLE = 20
        const val LOADING = 200

        /** Точка с двумя сотнями касс: её раскрытие не должно ломать колонку. */
        val CROWDED: RetailPlace = PlaceLook.place(1, registers = 200).copy(
            name = "Сауда орталығы «Достық Плаза» — бірінші қабаттағы азық-түлік бөлімі, " +
                "кассалар аймағы, шығысқа қарай екінші қатар"
        )

        val PLACES: List<RetailPlace> = listOf(
            CROWDED,
            PlaceLook.place(2, address = "Түркістан облысы, Сарыағаш ауданы, Жібек жолы көшесі, 147/3").copy(
                name = "Кәсіпкерлікқолдауорталығыныңжанындағыазықтүлікдүкені"
            )
        ) + (3..2004).map {
            PlaceLook.place(it, address = "Алматинская, Карасайский, Каскелен, Абылай хана, $it")
        }

        val REGISTERS: List<CabinetRegister> = (1..200).map { PlaceLook.register(it, CROWDED.id) }
    }
}
