package kz.mybrain.superkassa.presentation.analytics

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kz.mybrain.superkassa.CabinetRig
import kz.mybrain.superkassa.CabinetWire
import kz.mybrain.superkassa.KassaScene
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.data.analytics.CabinetReplies
import kz.mybrain.superkassa.domain.cabinet.model.CabinetCompany
import kz.mybrain.superkassa.domain.cabinet.model.CabinetUser
import kz.mybrain.superkassa.presentation.settings.look.lookModel
import kz.mybrain.superkassa.presentation.shell.ProvideWindowModels
import kz.mybrain.superkassa.presentation.shell.WindowModels
import kz.mybrain.superkassa.presentation.shell.bar.KkmTopBar
import kz.mybrain.superkassa.presentation.shell.frame.WindowParts
import kz.mybrain.superkassa.presentation.shell.frame.shellModel
import kz.mybrain.superkassa.presentation.shell.rail.SectionRail
import kz.mybrain.superkassa.presentation.shell.section.Section
import kz.mybrain.superkassa.presentation.shell.section.SectionContent
import kz.mybrain.superkassa.presentation.strings.common.Language
import kz.mybrain.superkassa.presentation.theme.Look
import kz.mybrain.superkassa.presentation.theme.color.Appearance
import java.io.File

/**
 * Окно кассы с открытой аналитикой кабинета — для замеров раскладки.
 *
 * Шапка, рельс и раздел те же, что в окне приложения: место, которое
 * достаётся аналитике, решают они, и мерить её без них — мерить не то.
 * Кабинет отвечает из [AnalyticsFixtures]; карта без плиток и без поиска
 * адресов — ни одного обращения в сеть.
 */
internal class AnalyticsWindow(
    val width: Int,
    val height: Int,
    val language: Language,
    look: Look = Look(),
    appearance: Appearance = Appearance.Light,
    reply: (String) -> String?
) : AutoCloseable {

    private val exchange = client(reply)

    /** Зависимости окна: аналитика ходит в тот же кабинет, что и разделы кабинета. */
    private val app = KassaScene.desk(KassaScene.kkm(shiftOpen = true)).app
        .analyzing(CabinetReplies.answering(reply).analytics)

    private val windowLook = lookModel(app)

    private val parts = WindowParts(shellModel(app), windowLook, CabinetRig(exchange, app).enter(OWNER, COMPANY).window)

    private val models = WindowModels()

    val probe = RenderProbe(width, height, appearance, look, language) { Window() }

    @Composable
    private fun Window() = ProvideWindowModels(models) {
        Frame()
    }

    @Composable
    private fun Frame() {
        val shell by parts.shell.state.collectAsState()
        Column(modifier = Modifier.fillMaxSize()) {
            KkmTopBar(shell, windowLook, onSignOut = {}, onRefresh = {})
            Row(modifier = Modifier.fillMaxSize()) {
                SectionRail(Section.entries, Section.Cabinet, false, {}, { Text("1.0.0") }) {}
                SectionContent(app, parts, Section.Cabinet)
            }
        }
    }

    /** Докручивает кадры: ответы кабинета приходят отложенными эффектами. */
    fun settle(frames: Int = SETTLE) = repeat(frames) { probe.frame() }

    /** Снимок в `/tmp/adaptive-analytics-<имя>.png`. */
    fun shot(name: String) {
        File("/tmp/adaptive-analytics-$name.png").writeBytes(probe.frame())
    }

    /** Узел с этой надписью или подписью значка; `null` — такого нет. */
    fun find(words: String, exact: Boolean = true): Seen? = all().firstOrNull {
        if (exact) it.words == words else it.words.contains(words)
    }

    fun all(): List<Seen> = probe.nodes { nodes -> nodes.map(::seen) }

    /** Нажать узел с надписью; нет такого — проверка падает с его именем. */
    fun click(words: String) {
        val node = requireNotNull(find(words)) { "в окне $width×$height нет «$words»" }
        probe.click(node.shown.center)
        settle()
    }

    override fun close() {
        probe.close()
        models.close()
    }

    private fun seen(node: SemanticsNode): Seen {
        val config = node.config
        val words = listOfNotNull(
            config.getOrNull(SemanticsProperties.Text)?.joinToString(" ") { it.text },
            config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString(" "),
            config.getOrNull(SemanticsProperties.EditableText)?.text,
            config.getOrNull(SemanticsProperties.TestTag)
        ).joinToString(" ")
        val at = node.positionInRoot
        val whole = Rect(at, Offset(at.x + node.size.width, at.y + node.size.height))
        return Seen(words, whole, node.boundsInRoot, config.getOrNull(SemanticsActions.OnClick) != null)
    }

    private fun client(reply: (String) -> String?): CabinetWire {
        val engine = MockEngine { request ->
            val query = request.url.encodedQuery.takeIf { it.isNotEmpty() }?.let { "?$it" }.orEmpty()
            val body = reply(request.url.encodedPath + query)
            if (body == null) {
                respond(NOT_FOUND, HttpStatusCode.NotFound, JSON)
            } else {
                respond(body, HttpStatusCode.OK, JSON)
            }
        }
        val http = HttpClient(engine) {
            expectSuccess = false
            install(ContentNegotiation) { json(CabinetWire.json) }
        }
        return CabinetWire(http = http)
    }

    private companion object {
        const val SETTLE = 40
        val JSON = headersOf(HttpHeaders.ContentType, "application/json")
        const val NOT_FOUND = """{"code":"NOT_FOUND","detail":"no such","status":404,"title":"Not Found"}"""
        val OWNER = CabinetUser(id = "u-1", iin = "920313351246", fullName = "Иванов Сергей")
        val COMPANY = CabinetCompany(id = "c-1", bin = "920313351246", name = "ТОО «Сеть касс»")
    }
}

/**
 * Узел окна так, как его меряют.
 *
 * @param whole место целиком — по положению в корне, не обрезанное видимой областью.
 * @param shown видимая часть: у элемента за краем окна она пуста.
 */
internal data class Seen(val words: String, val whole: Rect, val shown: Rect, val clickable: Boolean)
