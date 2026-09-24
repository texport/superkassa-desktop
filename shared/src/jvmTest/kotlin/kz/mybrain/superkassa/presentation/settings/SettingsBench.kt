package kz.mybrain.superkassa.presentation.settings

import io.github.texport.superkassa.testing.api.kassa.ReadyKassa
import io.github.texport.superkassa.testing.api.kassa.TestBench
import kotlinx.coroutines.Dispatchers
import kz.mybrain.superkassa.data.kassa.EmbeddedKassa
import kz.mybrain.superkassa.data.kassa.delivery.EmbeddedDeliveries
import kz.mybrain.superkassa.data.kassa.delivery.EmbeddedDeliverySetup
import kz.mybrain.superkassa.data.kassa.settings.EmbeddedSettings
import kz.mybrain.superkassa.domain.journal.port.JournalPorts
import kz.mybrain.superkassa.domain.kassa.port.KassaPorts
import kz.mybrain.superkassa.domain.log.port.Journal
import kz.mybrain.superkassa.domain.settings.port.SettingsPorts
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.domain.workplace.model.WorkplaceLook
import kz.mybrain.superkassa.kassa.MemoryLook
import kz.mybrain.superkassa.kassa.appBench
import kz.mybrain.superkassa.kassa.appKassa
import kz.mybrain.superkassa.presentation.analytics.analyticsPorts
import kz.mybrain.superkassa.presentation.common.message.Notices
import kz.mybrain.superkassa.presentation.common.model.Talk
import kz.mybrain.superkassa.presentation.shell.AppContainer
import kz.mybrain.superkassa.presentation.shell.AreaPorts
import kz.mybrain.superkassa.presentation.users.signin.loginModel
import kz.mybrain.superkassa.strings.api.Language
import java.io.File
import kotlin.io.path.createTempDirectory

/**
 * Настоящая касса на тестовом БФД, за которой вошёл администратор, —
 * настройки проверяются так, как их видит владелец на рабочем месте.
 *
 * Касса и её настройки — ядро в процессе на временном каталоге; память
 * рабочего места, принтер и выпуски — в памяти.
 * Журнал записывает всё, что в него пишут модели: проверка читает его
 * так же, как поддержка читает пересланный файл.
 */
class SettingsBench : AutoCloseable {
    private val directory: File = createTempDirectory("kassa-settings-").toFile()

    /**
     * Касса по протоколу тестового БФД: он говорит на CPCR 2.0.3, а запуск
     * приложения — на 2.0.4, и заведение кассы с ним БФД оснастки отвергает.
     */
    val bench: TestBench = appBench(directory)
    val kassa: ReadyKassa = bench.registerKassa(
        appKassa(adminPin = ADMIN_PIN, cashierPin = CASHIER_PIN, name = KKM_NAME).copy(cashierName = CASHIER_NAME)
    )
    val notices = Notices()
    val signIn = SignIn()
    val journal = RecordingJournal()
    val choices = MemoryChoices()
    val coreSettings = EmbeddedSettings(bench.superkassa.settings, io = Dispatchers.Unconfined)
    private val machine = settingsPorts()
    val app = AppContainer(
        kassa = EmbeddedKassa(bench.api, Dispatchers.Unconfined),
        signIn = signIn,
        memory = choices.memory,
        look = WorkplaceLook(MemoryLook()),
        talk = Talk(notices, journal) { Language.Ru },
        areas = AreaPorts(
            kassa = KassaPorts(EmbeddedDeliverySetup(bench.superkassa.settings, Dispatchers.Unconfined)),
            journal = JournalPorts(EmbeddedDeliveries(bench.superkassa.delivery, Dispatchers.Unconfined)),
            settings = SettingsPorts(coreSettings, choices),
            print = machine.print,
            update = machine.update,
            debug = machine.debug,
            analytics = analyticsPorts()
        )
    )

    /** Входит за кассу пином, как кассир на экране входа. */
    fun enter(pin: String = ADMIN_PIN): SettingsBench = apply {
        val login = loginModel(app)
        login.reload()
        login.pick(login.state.value.kkms.single())
        login.typePin(pin)
        login.enter()
        check(signIn.state.value.signedIn) { "sign in failed" }
    }

    private var restarted: TestBench? = null

    /**
     * Перезапуск кассы на том же каталоге, с тем же БФД и часами: то, что
     * касса читает только при запуске, вступает в силу.
     */
    fun restart(): TestBench {
        bench.close()
        return appBench(directory, bench.bfd, bench.clock).also { restarted = it }
    }

    override fun close() {
        (restarted ?: bench).close()
        directory.deleteRecursively()
    }

    /** Что попало в журнал рабочего места, строками. */
    class RecordingJournal : Journal {
        val lines = mutableListOf<String>()

        override fun info(text: String) {
            lines += text
        }

        override fun warn(text: String) {
            lines += text
        }

        override fun failure(text: String) {
            lines += text
        }
    }

    companion object {
        const val ADMIN_PIN = "7391"
        const val CASHIER_PIN = "4826"
        const val KKM_NAME = "Касса у входа"
        const val CASHIER_NAME = "Айгүл Сәрсенова"
    }
}
