package kz.mybrain.superkassa

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
import kz.mybrain.superkassa.domain.cabinet.model.CabinetCompany
import kz.mybrain.superkassa.domain.cabinet.model.CabinetUser
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.FakeCore
import kz.mybrain.superkassa.presentation.cabinet.CabinetProblem
import kz.mybrain.superkassa.presentation.cabinet.CabinetWindow
import kz.mybrain.superkassa.presentation.cabinet.cabinetMessage
import kz.mybrain.superkassa.presentation.settings.look.LookViewModel
import kz.mybrain.superkassa.presentation.shell.AppContainer
import kz.mybrain.superkassa.presentation.shell.ProvideWindowModels
import kz.mybrain.superkassa.presentation.shell.WindowModels
import kz.mybrain.superkassa.presentation.shell.frame.MessageEffect
import kz.mybrain.superkassa.presentation.shell.frame.MessageHost
import kz.mybrain.superkassa.presentation.strings.cabinet.cabinetTexts
import kz.mybrain.superkassa.presentation.strings.common.Language
import java.io.File

/**
 * Сцена кабинета для снимков: владелец уже вошёл, а сети нет.
 *
 * Кабинет владельца — рабочий, и ходить в него проверкой нельзя. Поэтому
 * доступ выдаётся здесь же — той же записью, какую кабинет отдаёт на вход
 * без ЭЦП, — а ответы подставляет [MockEngine] по пути запроса. Память
 * рабочего места — в памяти проверки: экраны кабинета её пишут.
 */
internal class CabinetStage(private val reply: (String) -> StubReply) {

    val app: AppContainer = CoreScene.app(FakeCore())

    private val rig = CabinetRig(client(), app)

    val cabinet: CabinetWindow = rig.window

    /** Вид окна, из которого собран [cabinet]: каркасу окна нужна модель целиком. */
    val look: LookViewModel get() = rig.look

    val texts = cabinetTexts(Language.Ru)

    /** Модели окна: разделы кабинета берут свои модели у окна, как в приложении. */
    val models = WindowModels()

    init {
        rig.enter(OWNER, COMPANY)
    }

    /**
     * Ждёт, пока кабинет прочтёт хозяйство вошедшего: чтение начинается само,
     * как только владелец вошёл, и идёт своим чередом.
     */
    fun settled() {
        val until = System.nanoTime() + SETTLE_NANOS
        while (!cabinet.cabinet.state.value.placesRead && System.nanoTime() < until) Thread.sleep(SETTLE_STEP)
    }

    /** Содержимое в окне сцены: с его моделями. */
    @Composable
    fun Window(content: @Composable () -> Unit) = ProvideWindowModels(models, content)

    private fun client(): CabinetWire {
        val engine = MockEngine { request ->
            val answer = reply(request.url.encodedPath)
            respond(answer.body, answer.status, jsonHeader)
        }
        val http = HttpClient(engine) {
            expectSuccess = false
            install(ContentNegotiation) { json(CabinetWire.json) }
        }
        return CabinetWire(http = http)
    }

    private companion object {
        const val SETTLE_NANOS = 5_000_000_000L
        const val SETTLE_STEP = 10L
        val jsonHeader = headersOf(HttpHeaders.ContentType, "application/json")
        val OWNER = CabinetUser(id = "u-1", iin = "900101300000", fullName = "Курманов Азамат Бахытжанович")
        val COMPANY = CabinetCompany(id = "c-1", bin = "230140000000", name = "ТОО «Азик и Ко»")
    }
}

/** Ответ кабинета на один путь: тем же телом и тем же кодом, что по сети. */
internal data class StubReply(val body: String, val status: HttpStatusCode = HttpStatusCode.OK)

/** Отказ кабинета: код и слова, как в ответе по RFC 9457. */
internal fun refusal(code: String, detail: String, status: HttpStatusCode) = StubReply(
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
