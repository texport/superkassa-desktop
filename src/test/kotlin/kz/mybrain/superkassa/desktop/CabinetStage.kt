package kz.mybrain.superkassa.desktop

import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kz.mybrain.superkassa.desktop.app.CabinetProblem
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.Preferences
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.ServerClient
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetClient
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetCompany
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetMe
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetUser
import kz.mybrain.superkassa.desktop.ui.MessageEffect
import kz.mybrain.superkassa.desktop.ui.MessageHost
import kz.mybrain.superkassa.desktop.ui.cabinet.cabinetMessage
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.cabinetTexts
import java.io.File
import java.nio.file.Files

/**
 * Сцена кабинета для снимков: владелец уже вошёл, а сети нет.
 *
 * Кабинет владельца и узел на этой машине — рабочие, и ходить в них
 * проверкой нельзя. Поэтому доступ выдаётся здесь же — той же записью,
 * какую кабинет отдаёт на вход без ЭЦП, — а ответы подставляет
 * [MockEngine] по пути запроса. Настройки читаются из своего временного
 * каталога: экраны кабинета их пишут, и общий каталог задел бы настройки
 * машины.
 */
internal class CabinetStage(private val reply: (String) -> CabinetReply) {

    val session: Session = Session(
        ServerClient(http = HttpClient(MockEngine { respond("{}", HttpStatusCode.OK, jsonHeader) })),
        Preferences(File(Files.createTempDirectory("cabinet-shot").toFile(), "kkm"))
    )

    val cabinet: CabinetSession = CabinetSession(client())

    val texts = cabinetTexts(Language.Ru)

    init {
        // Язык рабочего места ставится тот же, что подставляет сцена:
        // по умолчанию у нового места он казахский, и экран выходил
        // наполовину русским, наполовину казахским — из-за оснастки,
        // а не из-за кабинета.
        session.switchLanguage(Language.Ru)
        cabinet.access.enter(ACCESS, CabinetMe(user = OWNER, company = COMPANY))
    }

    private fun client(): CabinetClient {
        val engine = MockEngine { request ->
            val answer = reply(request.url.encodedPath)
            respond(answer.body, answer.status, jsonHeader)
        }
        val http = HttpClient(engine) {
            expectSuccess = false
            install(ContentNegotiation) { json(CabinetClient.lenientJson) }
        }
        return CabinetClient(http = http)
    }

    private companion object {
        val jsonHeader = headersOf(HttpHeaders.ContentType, "application/json")
        const val ACCESS = "shot-access"
        val OWNER = CabinetUser(id = "u-1", iin = "900101300000", fullName = "Курманов Азамат Бахытжанович")
        val COMPANY = CabinetCompany(id = "c-1", bin = "230140000000", name = "ТОО «Азик и Ко»")
    }
}

/** Ответ кабинета на один путь: тем же телом и тем же кодом, что по сети. */
internal data class CabinetReply(val body: String, val status: HttpStatusCode = HttpStatusCode.OK)

/** Отказ кабинета: код и слова, как в ответе по RFC 9457. */
internal fun refusal(code: String, detail: String, status: HttpStatusCode) = CabinetReply(
    """{"code":"$code","detail":"$detail","status":${status.value},"title":"${status.description}"}""",
    status
)

/**
 * Помеха кабинета так, как её видит владелец: всплывающей строкой окна.
 *
 * Отказ входа, недоступный кабинет и неотвеченный NCALayer показываются
 * не на месте содержимого, а снекбаром каркаса. Снимать их поверх экрана
 * входа — единственный способ увидеть то, что видит владелец.
 */
@Composable
internal fun WithCabinetMessage(problem: CabinetProblem, content: @Composable () -> Unit) {
    val messages = remember { SnackbarHostState() }
    Scaffold(snackbarHost = { MessageHost(messages) }) {
        MessageEffect(cabinetMessage(problem, cabinetTexts(Language.Ru)), messages) {}
        content()
    }
}

/**
 * Снимок экрана в файл.
 *
 * Кадров нужно много: списки и карточки кабинета приходят отложенным
 * эффектом, и на первом кадре экран ещё пуст. Сцена отдаёт кадры только
 * по запросу, поэтому очередь обращений докручивается вручную.
 */
internal fun shot(name: String, width: Int = 1180, height: Int = 820, content: @Composable () -> Unit): ByteArray {
    RenderProbe(width = width, height = height, content = content).use { probe ->
        var frame = probe.frame()
        repeat(60) { frame = probe.frame() }
        File("/tmp/cabinet-$name.png").writeBytes(frame)
        return frame
    }
}
