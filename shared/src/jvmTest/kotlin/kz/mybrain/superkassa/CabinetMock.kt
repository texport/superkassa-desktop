package kz.mybrain.superkassa

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.MutableStateFlow
import kz.mybrain.superkassa.domain.cabinet.model.CabinetCompany
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.domain.cabinet.model.CabinetUser
import kz.mybrain.superkassa.domain.cabinet.model.EdsProblem
import kz.mybrain.superkassa.domain.cabinet.model.EdsRefusal
import kz.mybrain.superkassa.domain.cabinet.port.CabinetPorts
import kz.mybrain.superkassa.domain.cabinet.port.SavedFiles
import kz.mybrain.superkassa.domain.cabinet.port.Signer
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.FakeCore
import kz.mybrain.superkassa.presentation.cabinet.CabinetCases
import kz.mybrain.superkassa.presentation.cabinet.CabinetLists
import kz.mybrain.superkassa.presentation.cabinet.CabinetUiState
import kz.mybrain.superkassa.presentation.cabinet.CabinetViewModel
import kz.mybrain.superkassa.presentation.cabinet.CabinetWindow
import kz.mybrain.superkassa.presentation.cabinet.CabinetWork
import kz.mybrain.superkassa.presentation.cabinet.register.RegisterUiState
import kz.mybrain.superkassa.presentation.cabinet.register.RegisterView
import kz.mybrain.superkassa.presentation.settings.look.LookViewModel
import kz.mybrain.superkassa.presentation.settings.look.lookModel
import kz.mybrain.superkassa.presentation.shell.AppContainer
import kz.mybrain.superkassa.presentation.shell.ProvideWindowModels
import kz.mybrain.superkassa.presentation.shell.WindowModels
import kz.mybrain.superkassa.presentation.shell.frame.cabinetLook

/**
 * Кабинет окна для проверок — так же собранный, как в приложении:
 * порты на модуле кабинета, модель кабинета и то, что разделам нужно от окна.
 *
 * Ответы задаёт обмен [client] — подставной кабинет проверки; вход — как
 * у [SignedCabinet].
 */
internal class CabinetRig(
    client: CabinetWire = CabinetWire(http = replying("{}")),
    val app: AppContainer = CoreScene.app(FakeCore()),
    signer: Signer = NoSigner
) {
    private val cabinet = SignedCabinet(client, signer)
    val files: KeptFiles get() = cabinet.files
    val ports: CabinetPorts get() = cabinet.ports
    val model = CabinetViewModel(cabinetCases(app, ports), app.talk)

    /** Вид окна: кабинет получает от него только своё, а каркасу нужна сама модель. */
    val look = lookModel(app)
    val window = CabinetWindow(app, model, cabinetLook(look))

    /** Владелец вошёл. */
    fun enter(user: CabinetUser = OWNER, company: CabinetCompany = COMPANY): CabinetRig {
        cabinet.enter(user, company)
        return this
    }

    companion object {
        /** ИИН владельца и БИН его компании: подставные, но казахстанского вида. */
        val OWNER = CabinetUser(id = "u-1", iin = "870101300123", fullName = "Иванов Сергей")
        val COMPANY = CabinetCompany(id = "c-1", bin = "180140000123", name = "ТОО «Пример»")
    }
}

/** Порты кабинета поверх обмена проверки — от имени вошедшего владельца, без модели окна. */
internal fun CabinetWire.signedPorts(): CabinetPorts = SignedCabinet(this).also { it.enter() }.ports

/** Сценарии кабинета над портами проверки и кассой её контейнера. */
internal fun cabinetCases(app: AppContainer, ports: CabinetPorts) =
    CabinetCases(app.kassa, app.signIn, app.memory, ports)

/**
 * Кабинет, отвечающий заданным, — для снимков отказных состояний.
 *
 * @param body тело ответа на любой запрос.
 * @param status состояние ответа: `404` означает невыложенный раздел,
 *   прочие отказы — отказ кабинета по существу.
 */
internal fun mockCabinet(body: String, status: HttpStatusCode = HttpStatusCode.OK): CabinetWindow =
    mockCabinet(CabinetWire(http = replying(body, status)))

/** Вошедший кабинет поверх своего обмена: ответы задаёт вызывающий. */
internal fun mockCabinet(client: CabinetWire): CabinetWindow = CabinetRig(client).enter().window

/** Кабинет, в который никто не входил: окно кассы без владельца. */
internal fun idleCabinet(app: AppContainer = CoreScene.app(FakeCore()), look: LookViewModel = lookModel(app)) =
    CabinetWindow(app, CabinetRig(app = app).model, cabinetLook(look))

/**
 * Чтение хозяйства кабинета без модели окна: вошедший владелец, списки
 * и состояние, в которое они ложатся. Модель читала бы хозяйство сама,
 * как только владелец вошёл, — проверке списков это мешало бы.
 */
internal class CabinetListsRig(client: CabinetWire, app: AppContainer = CoreScene.app(FakeCore())) {
    val screen = MutableStateFlow(CabinetUiState())

    // Порты без модели окна: модель, увидев вошедшего, читала бы хозяйство
    // сама, в своём потоке, — и её страницы смешивались с проверяемыми.
    private val ports = SignedCabinet(client).also { it.enter() }.ports
    val lists = CabinetLists(app.talk, cabinetCases(app, ports), CabinetWork(app.talk), screen)

    val state: CabinetUiState get() = screen.value
}

/** Обмен, отвечающий на всё одним и тем же. */
internal fun replying(body: String, status: HttpStatusCode = HttpStatusCode.OK): HttpClient =
    jsonHttp(MockEngine { respond(body, status, headersOf(HttpHeaders.ContentType, "application/json")) })

/** Обмен поверх [engine] так, как его ведёт кабинет: отказ читается телом, а не исключением. */
internal fun jsonHttp(engine: MockEngine): HttpClient = HttpClient(engine) {
    expectSuccess = false
    install(ContentNegotiation) { json(CabinetWire.json) }
}

/** Подписывающий, которого нет: в проверках без NCALayer подпись не получить. */
internal object NoSigner : Signer {
    override suspend fun sign(payload: String): String =
        throw EdsRefusal(EdsProblem.Unreachable, Signer.NO_HANDSHAKE)
}

/** Сохранённые файлы — в памяти проверки, без окна выбора. */
internal class KeptFiles : SavedFiles {
    val saved = mutableMapOf<String, ByteArray>()

    override suspend fun save(bytes: ByteArray, name: String): String {
        saved[name] = bytes
        return name
    }
}

/** Содержимое с моделями окна: разделы кабинета берут свои модели у окна, как в приложении. */
@Composable
internal fun Windowed(content: @Composable () -> Unit) = ProvideWindowModels(remember { WindowModels() }, content)

/** Касса в карточке так, как её видит модель: и кассы этой машины, если [here] заведена здесь. */
internal fun viewOf(register: CabinetRegister, here: KkmResponse? = null) =
    RegisterView(register, register, RegisterUiState(card = register, kkms = listOfNotNull(here), kkmsRead = true))
