package kz.mybrain.superkassa.desktop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetClient
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetCompany
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetMe
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetUser
import kz.mybrain.superkassa.desktop.ui.KkmBarActions
import kz.mybrain.superkassa.desktop.ui.Section
import kz.mybrain.superkassa.desktop.ui.SectionContent
import kz.mybrain.superkassa.desktop.ui.SectionRail
import kz.mybrain.superkassa.desktop.ui.cabinet.CabinetDocuments
import kz.mybrain.superkassa.desktop.ui.components.AppTopBar
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.theme.Appearance
import kz.mybrain.superkassa.desktop.ui.theme.Look
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

    private val session: Session = KassaScene.session("an-window-$width-$height", shift = KassaScene.openShift())
        .also { session ->
            session.switchLanguage(language)
            session.preferences.maps.tiles = "file:///superkassa-no-tiles"
            session.preferences.maps.search = "file:///superkassa-no-search"
            session.preferences.maps.reverse = "file:///superkassa-no-search"
            session.preferences.maps.location = "file:///superkassa-no-search"
        }

    private val cabinet = CabinetSession(client(reply)).also {
        it.access.enter("window-access", CabinetMe(user = OWNER, company = COMPANY))
    }

    val probe = RenderProbe(width, height, appearance, look, language) { Window() }

    @Composable
    private fun Window() {
        val kkm = session.selected
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar(
                title = kkm?.let { session.displayName(it) }.orEmpty(),
                subtitle = kkm?.orgTitle,
                subtitleKept = session.whoami?.name
            ) { KkmBarActions(session, onSignOut = {}, onRefresh = {}) }
            Row(modifier = Modifier.fillMaxSize()) {
                SectionRail(Section.entries, Section.Cabinet, false, {}, { Text("1.0.0") }) {}
                SectionContent(session, cabinet, CabinetDocuments(), Section.Cabinet)
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

    override fun close() = probe.close()

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

    private fun client(reply: (String) -> String?): CabinetClient {
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
            install(ContentNegotiation) { json(CabinetClient.lenientJson) }
        }
        return CabinetClient(http = http)
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
