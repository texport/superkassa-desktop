package kz.mybrain.superkassa

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.ktor.http.HttpStatusCode
import kz.mybrain.superkassa.domain.cabinet.model.CabinetCompany
import kz.mybrain.superkassa.domain.cabinet.model.CabinetUser
import kz.mybrain.superkassa.domain.cabinet.port.CabinetPorts
import kz.mybrain.superkassa.domain.cabinet.port.Signer
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.FakeCore
import kz.mybrain.superkassa.kassa.services
import kz.mybrain.superkassa.presentation.cabinet.CabinetNeighbours
import kz.mybrain.superkassa.presentation.cabinet.CabinetWindow
import kz.mybrain.superkassa.presentation.cabinet.cabinetLook
import kz.mybrain.superkassa.presentation.cabinet.cabinetModel
import kz.mybrain.superkassa.presentation.common.look.LookViewModel
import kz.mybrain.superkassa.presentation.common.look.lookModel
import kz.mybrain.superkassa.presentation.common.model.ProvideWindowModels
import kz.mybrain.superkassa.presentation.common.model.WindowModels
import kz.mybrain.superkassa.presentation.common.model.WindowServices

/**
 * Кабинет окна для проверок — так же собранный, как в приложении:
 * порты на модуле кабинета, модель кабинета и то, что разделам нужно от окна.
 *
 * Ответы задаёт обмен [client] — подставной кабинет проверки; вход — как
 * у [SignedCabinet]. Соседей — аналитику, окно карты, память мастера —
 * подставляет тот, кто собирает окно; без них вкладка аналитики пуста.
 */
class CabinetRig(
    client: CabinetWire = CabinetWire(http = replying("{}")),
    val services: WindowServices = CoreScene.services(FakeCore()),
    signer: Signer = NoSigner,
    neighbours: CabinetNeighbours = CabinetNeighbours()
) {
    private val cabinet = SignedCabinet(client, signer)
    val files: KeptFiles get() = cabinet.files
    val ports: CabinetPorts get() = cabinet.ports
    val model = cabinetModel(services, ports)

    /** Вид окна: кабинет получает от него только своё, а каркасу нужна сама модель. */
    val look = lookModel(services.look)
    val window = CabinetWindow(model, cabinetLook(look), neighbours)

    /** Владелец вошёл. */
    fun enter(user: CabinetUser = SignedCabinet.OWNER, company: CabinetCompany = SignedCabinet.COMPANY): CabinetRig {
        cabinet.enter(user, company)
        return this
    }
}

/**
 * Кабинет, отвечающий заданным, — для снимков отказных состояний.
 *
 * @param body тело ответа на любой запрос.
 * @param status состояние ответа: `404` означает невыложенный раздел,
 *   прочие отказы — отказ кабинета по существу.
 */
fun mockCabinet(body: String, status: HttpStatusCode = HttpStatusCode.OK): CabinetWindow =
    mockCabinet(CabinetWire(http = replying(body, status)))

/** Вошедший кабинет поверх своего обмена: ответы задаёт вызывающий. */
fun mockCabinet(client: CabinetWire): CabinetWindow = CabinetRig(client).enter().window

/** Кабинет, в который никто не входил: окно кассы без владельца. */
fun idleCabinet(
    services: WindowServices = CoreScene.services(FakeCore()),
    look: LookViewModel = lookModel(services.look),
    neighbours: CabinetNeighbours = CabinetNeighbours()
) = CabinetWindow(CabinetRig(services = services).model, cabinetLook(look), neighbours)

/** Содержимое с моделями окна: разделы кабинета берут свои модели у окна, как в приложении. */
@Composable
fun Windowed(content: @Composable () -> Unit) = ProvideWindowModels(remember { WindowModels() }, content)
