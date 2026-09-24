package kz.mybrain.superkassa

import android.app.Application
import android.util.Log
import io.github.texport.superkassa.embedded.api.Superkassa
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kz.mybrain.superkassa.background.BackgroundWork
import kz.mybrain.superkassa.data.analytics.AnalyticsNotOnAndroid
import kz.mybrain.superkassa.data.analytics.MapsNotOnAndroid
import kz.mybrain.superkassa.data.analytics.ProcessMapMemory
import kz.mybrain.superkassa.data.kassa.EmbeddedKassa
import kz.mybrain.superkassa.data.kassa.delivery.EmbeddedDeliveries
import kz.mybrain.superkassa.data.kassa.delivery.EmbeddedDeliverySetup
import kz.mybrain.superkassa.data.kassa.settings.EmbeddedSettings
import kz.mybrain.superkassa.data.local.AndroidChoices
import kz.mybrain.superkassa.data.local.AndroidWorkplace
import kz.mybrain.superkassa.data.local.ForegroundActivity
import kz.mybrain.superkassa.data.log.LogcatBook
import kz.mybrain.superkassa.data.log.LogcatJournal
import kz.mybrain.superkassa.data.print.AndroidPrintChoices
import kz.mybrain.superkassa.data.print.SystemDialogPrintOut
import kz.mybrain.superkassa.data.releases.StoreReleases
import kz.mybrain.superkassa.domain.journal.port.JournalPorts
import kz.mybrain.superkassa.domain.kassa.port.KassaPorts
import kz.mybrain.superkassa.domain.setup.port.SetupPorts
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.domain.workplace.model.WorkplaceLook
import kz.mybrain.superkassa.presentation.analytics.AnalyticsPorts
import kz.mybrain.superkassa.presentation.common.mapview.MapPorts
import kz.mybrain.superkassa.presentation.common.message.Notices
import kz.mybrain.superkassa.presentation.common.model.Talk
import kz.mybrain.superkassa.presentation.settings.SettingsPorts
import kz.mybrain.superkassa.presentation.shell.AppContainer
import kz.mybrain.superkassa.presentation.shell.AreaPorts
import kz.mybrain.superkassa.presentation.strings.common.Language
import java.io.File

/**
 * Точка сборки кассы на Android — единственное место, знающее все три слоя.
 *
 * Касса живёт столько же, сколько процесс, а не активность: поворот экрана
 * пересоздаёт активность, а второй экземпляр кассы на том же каталоге
 * ядро не откроет. Поднимается она вне главного потока — при открытии
 * ядро сверяет часы с эталоном в сети.
 *
 * Экраны и фоновая работа берут один и тот же экземпляр: процесс, поднятый
 * WorkManager без экранов, проходит через этот же `onCreate`, и замок
 * каталога кассы второй раз не берётся.
 */
class SuperkassaApp : Application() {
    private val scope: CoroutineScope = MainScope()

    /** Касса процесса; готова, когда ядро открыло базу и сверило часы. */
    lateinit var kassa: Deferred<Superkassa>
        private set

    /** Зависимости экранов; готовы, когда касса поднялась. */
    lateinit var container: Deferred<AppContainer>
        private set

    /** Активность на экране: над ней открываются диалог печати и окно «Сохранить». */
    private lateinit var screen: ForegroundActivity

    override fun onCreate() {
        super.onCreate()
        screen = ForegroundActivity(this)
        kassa = scope.async(Dispatchers.IO) { open() }
        container = scope.async(Dispatchers.IO) { assemble(kassa.await()) }
        BackgroundWork.schedule(this)
    }

    /** Касса процесса; не открылась — в журнал только что и почему, без путей и документов. */
    private fun open(): Superkassa = runCatching { KassaSource.open(this) }
        .onFailure { Log.w(TAG, "kassa did not open: ${it.javaClass.simpleName}") }
        .getOrThrow()

    private fun assemble(kassa: Superkassa): AppContainer {
        // Настройки и кассиры: выпуски на Android приносит магазин приложений.
        val updates = StoreReleases()
        val workplace = AndroidWorkplace(this)
        val look = WorkplaceLook(workplace)
        val log = LogcatBook(File(filesDir, LOG_DIRECTORY).path, screen)
        return AppContainer(
            kassa = EmbeddedKassa(kassa.api),
            signIn = SignIn(),
            memory = workplace,
            look = look,
            // Слова кассиру — на языке окна, как на компьютере.
            talk = Talk(Notices(), LogcatJournal(log)) { Language.byCode(look.state.value.language) },
            // Кабинета на Android нет: подписи ЭЦП здесь пока нет. Мастер
            // подключения без кабинета ведёт ручной путь — идентификатор и токен.
            areas = AreaPorts(
                kassa = KassaPorts(EmbeddedDeliverySetup(kassa.settings)),
                journal = JournalPorts(EmbeddedDeliveries(kassa.delivery)),
                settings = settingsPorts(kassa, updates, workplace, log),
                analytics = analyticsPorts(),
                setup = SetupPorts(memory = workplace)
            )
        )
    }

    /** Настройки, печать, обновление и журнал отладки на Android. */
    private fun settingsPorts(
        kassa: Superkassa,
        updates: StoreReleases,
        workplace: AndroidWorkplace,
        log: LogcatBook
    ) = SettingsPorts(
        logBook = log,
        releases = updates,
        updateMemory = updates,
        printOut = SystemDialogPrintOut(screen),
        printChoices = AndroidPrintChoices(this),
        coreSettings = EmbeddedSettings(kassa.settings, KassaSource.directory(this).path),
        workplace = AndroidChoices(workplace)
    )

    /** Аналитики кабинета и карты на Android пока нет: порты отвечают, что раздела нет. */
    private fun analyticsPorts() = AnalyticsPorts(
        cabinet = AnalyticsNotOnAndroid(),
        map = MapPorts(MapsNotOnAndroid(), ProcessMapMemory())
    )
}

private const val TAG = "Superkassa"

/** Каталог файлов журнала в памяти приложения — как `log` рядом с настройками на компьютере. */
private const val LOG_DIRECTORY = "log"
