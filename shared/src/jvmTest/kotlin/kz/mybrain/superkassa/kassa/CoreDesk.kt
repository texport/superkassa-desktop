package kz.mybrain.superkassa.kassa

import io.github.texport.superkassa.delivery.api.port.DeliveryPort
import io.github.texport.superkassa.testing.api.bfd.FakeBfd
import io.github.texport.superkassa.testing.api.kassa.ReadyKassa
import io.github.texport.superkassa.testing.api.kassa.TestBench
import io.github.texport.superkassa.testing.api.kassa.VatMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kz.kazakhtelecom.proto.v203.Money
import kz.mybrain.superkassa.data.kassa.EmbeddedKassa
import kz.mybrain.superkassa.data.kassa.delivery.EmbeddedDeliverySetup
import kz.mybrain.superkassa.domain.kassa.port.KassaPorts
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.presentation.common.message.Message
import kz.mybrain.superkassa.presentation.common.message.Notices
import kz.mybrain.superkassa.presentation.shell.AppContainer
import java.io.File
import kotlin.io.path.createTempDirectory

/**
 * Рабочее место кассира на настоящем ядре и тестовом БФД.
 *
 * Экраны собираются той же точкой сборки, что в окне ([CoreScene.app]),
 * а касса заводится через фасад, как у владельца. Итог сценария
 * сверяется и с ядром — [kassa] и его документы, — и с тем, что ушло
 * в БФД, — [bfd]. Каталог временный: рабочее место машины не трогается.
 *
 * Главный поток подменяется на месте: модели зовут кассу из
 * `viewModelScope`, и действие кассира кончается до следующей строки проверки.
 *
 * @param channels каналы доставки чека покупателю, заказанные до открытия
 *   кассы, — например, подменный SMS; пусто — доставки нет.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CoreDesk(channels: List<DeliveryPort> = emptyList()) : AutoCloseable {
    private val directory: File = createTempDirectory("kassa-desk-").toFile()
    val bench: TestBench
    val notices = Notices()
    val signIn = SignIn()
    val memory = MemoryWorkplace()

    init {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val bfd = FakeBfd()
        if (channels.isNotEmpty()) orderDelivery(directory, bfd, channels)
        bench = appBench(directory, bfd, channels = channels)
    }

    val bfd: FakeBfd get() = bench.bfd

    /** Зависимости экранов окна поверх кассы процесса. */
    val app: AppContainer by lazy {
        val delivery = KassaPorts(EmbeddedDeliverySetup(bench.superkassa.settings, Dispatchers.Unconfined))
        CoreScene.app(EmbeddedKassa(bench.api, Dispatchers.Unconfined), signIn, notices, memory, ports = delivery)
    }

    /** Заводит кассу с пинами [ADMIN_PIN] и [CASHIER_PIN]. */
    fun register(vat: VatMode = VatMode.NotPayer, name: String? = "Касса у входа"): ReadyKassa =
        bench.registerKassa(appKassa(adminPin = ADMIN_PIN, cashierPin = CASHIER_PIN, vat = vat, name = name))

    /** Касса с открытой сменой, за которой уже сидит кассир (или администратор). */
    fun seated(vat: VatMode = VatMode.NotPayer, admin: Boolean = false): ReadyKassa =
        register(vat).also { it.openShift() }.also { sit(it, admin) }

    /** Кассир входит пином, как на экране входа: пин проверяет касса. */
    fun sit(kassa: ReadyKassa, admin: Boolean = false) {
        val pin = if (admin) kassa.adminPin else kassa.cashierPin
        signIn.enter(kassa.info(), bench.api.authenticate(kassa.kkmId, pin), pin)
    }

    /** Последнее сообщение кассиру — итог, отказ или «нет ответа». */
    val said: Message? get() = notices.last

    /** Текст последнего сообщения, каким его видит кассир. */
    val saidText: String
        get() = when (val message = notices.last) {
            is Message.Done -> message.text
            is Message.Refusal -> message.text
            is Message.NoAnswer -> message.what
            is Message.Failed -> message.what
            null -> ""
        }

    override fun close() {
        bench.close()
        directory.deleteRecursively()
        Dispatchers.resetMain()
    }

    companion object {
        const val ADMIN_PIN = "7391"
        const val CASHIER_PIN = "4826"
    }
}

/** Сумма протокола БФД в тиынах: тенге и тиыны отдельно. */
fun Money?.tiyn(): Long = this?.let { it.bills * TIYN_IN_TENGE + it.coins } ?: 0L

private const val TIYN_IN_TENGE = 100L
