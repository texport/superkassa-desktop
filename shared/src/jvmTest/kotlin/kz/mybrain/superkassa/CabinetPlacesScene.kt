package kz.mybrain.superkassa

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.KSerializer
import kz.mybrain.superkassa.data.cabinet.CabinetClient
import kz.mybrain.superkassa.data.cabinet.CabinetPage
import kz.mybrain.superkassa.data.cabinet.CabinetRegister
import kz.mybrain.superkassa.data.cabinet.RetailPlace
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.presentation.Section
import kz.mybrain.superkassa.presentation.SectionContent
import kz.mybrain.superkassa.presentation.SectionRail
import kz.mybrain.superkassa.presentation.cabinet.CabinetDocuments
import kz.mybrain.superkassa.presentation.components.AppTopBar
import kz.mybrain.superkassa.presentation.strings.Language
import kz.mybrain.superkassa.presentation.strings.cabinetTexts
import kz.mybrain.superkassa.presentation.theme.Look
import kz.mybrain.superkassa.presentation.theme.TextScale
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
    val texts = cabinetTexts(language)
    private val stage = CabinetStage(::reply)

    init {
        stage.session.switchLanguage(language)
    }

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
                val app = CoreScene.app(stage.session)
                SectionContent(stage.session, app, stage.cabinet, CabinetDocuments(), Section.Cabinet)
            }
        }
    }

    private fun reply(path: String): CabinetReply = when (path) {
        "/api/retail-places" -> CabinetReply(page(RetailPlace.serializer(), PLACES))
        "/api/cash-registers" -> CabinetReply(page(CabinetRegister.serializer(), REGISTERS))
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

        private fun <T> page(item: KSerializer<T>, items: List<T>): String =
            CabinetClient.lenientJson.encodeToString(
                CabinetPage.serializer(item),
                CabinetPage(page = 0, size = items.size, totalElements = items.size.toLong(), items = items)
            )
    }
}

/** Нажатие в середину первого узла, подходящего под условие. */
internal fun RenderProbe.tap(pick: (ProbeNode) -> Boolean) {
    val node = nodes().first(pick)
    click(node.at + Offset(node.width / 2f, node.height / 2f))
}
