package kz.mybrain.superkassa

import android.app.Application
import android.util.Log
import io.github.texport.superkassa.embedded.api.Superkassa
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.io.files.Path
import kz.mybrain.superkassa.background.BackgroundWork
import kz.mybrain.superkassa.data.analytics.CabinetAnalytics
import kz.mybrain.superkassa.data.analytics.MapsNotOnAndroid
import kz.mybrain.superkassa.data.cabinet.RemoteCabinet
import kz.mybrain.superkassa.data.cabinet.setup.CabinetSetup
import kz.mybrain.superkassa.data.kassa.EmbeddedKassa
import kz.mybrain.superkassa.data.kassa.delivery.EmbeddedDeliveries
import kz.mybrain.superkassa.data.kassa.delivery.EmbeddedDeliverySetup
import kz.mybrain.superkassa.data.kassa.settings.EmbeddedSettings
import kz.mybrain.superkassa.data.local.DocumentFiles
import kz.mybrain.superkassa.data.local.ForegroundActivity
import kz.mybrain.superkassa.data.local.workplace.PreferenceChoices
import kz.mybrain.superkassa.data.local.workplace.Preferences
import kz.mybrain.superkassa.data.local.workplace.SystemLanguageLook
import kz.mybrain.superkassa.data.log.AppJournal
import kz.mybrain.superkassa.data.log.AppLog
import kz.mybrain.superkassa.data.log.AppLogBook
import kz.mybrain.superkassa.data.log.LogSettings
import kz.mybrain.superkassa.data.log.LogSource
import kz.mybrain.superkassa.data.log.LogcatJournal
import kz.mybrain.superkassa.data.map.WorkplaceMapMemory
import kz.mybrain.superkassa.data.print.SystemDialogPrintOut
import kz.mybrain.superkassa.data.releases.StoreReleases
import kz.mybrain.superkassa.domain.debug.port.DebugPorts
import kz.mybrain.superkassa.domain.journal.port.JournalPorts
import kz.mybrain.superkassa.domain.kassa.port.KassaPorts
import kz.mybrain.superkassa.domain.print.port.PrintPorts
import kz.mybrain.superkassa.domain.settings.port.SettingsPorts
import kz.mybrain.superkassa.domain.setup.port.SetupPorts
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.domain.update.port.UpdatePorts
import kz.mybrain.superkassa.domain.workplace.model.WorkplaceLook
import kz.mybrain.superkassa.presentation.analytics.AnalyticsPorts
import kz.mybrain.superkassa.presentation.common.mapview.MapPorts
import kz.mybrain.superkassa.presentation.common.message.Notices
import kz.mybrain.superkassa.presentation.common.model.Talk
import kz.mybrain.superkassa.presentation.common.model.WindowServices
import kz.mybrain.superkassa.presentation.common.strings.workplaceLanguage
import kz.mybrain.superkassa.presentation.shell.AppContainer
import kz.mybrain.superkassa.presentation.shell.AreaPorts
import kz.mybrain.superkassa.strings.api.Language

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

    /**
     * Каталог данных рабочего места — внутренняя память приложения: настройки,
     * журнал и касса лежат в нём так же, как в каталоге данных на компьютере.
     */
    private val home: Path get() = Path(filesDir.path)

    override fun onCreate() {
        super.onCreate()
        screen = ForegroundActivity(this)
        kassa = scope.async(Dispatchers.IO) {
            // Журнал поднимается до первого обращения к кассе, как на компьютере.
            AppLog.start(LogSettings(home))
            open()
        }
        container = scope.async(Dispatchers.IO) { assemble(kassa.await()) }
        BackgroundWork.schedule(this)
    }

    /** Касса процесса; не открылась — в журнал только что и почему, без путей и документов. */
    private fun open(): Superkassa = runCatching { KassaSource.open(this) }
        .onFailure { Log.w(TAG, "kassa did not open: ${it.javaClass.simpleName}") }
        .getOrThrow()

    private fun assemble(kassa: Superkassa): AppContainer {
        val preferences = Preferences(home)
        val look = WorkplaceLook(SystemLanguageLook(this, preferences))
        return AppContainer(
            services = WindowServices(
                kassa = EmbeddedKassa(kassa.api),
                signIn = SignIn(),
                memory = preferences,
                look = look,
                // Слова кассиру — на языке окна, как на компьютере.
                talk = Talk(Notices(), LogcatJournal()) { workplaceLanguage(look.state.value.language) }
            ),
            areas = areaPorts(kassa, preferences) { workplaceLanguage(look.state.value.language) }
        )
    }

    /**
     * Порты областей на Android.
     *
     * Кабинет — тот же, что на компьютере, с подписью eGov mobile или файлом
     * ключа ([androidCabinet]); мастер подключения ведёт и путь через кабинет,
     * и ручной — идентификатор и токен.
     */
    private fun areaPorts(kassa: Superkassa, preferences: Preferences, language: () -> Language): AreaPorts {
        // Выпуски на Android приносит магазин приложений.
        val updates = StoreReleases()
        val cabinet = androidCabinet(preferences, screen, language)
        return AreaPorts(
            kassa = KassaPorts(EmbeddedDeliverySetup(kassa.settings)),
            journal = JournalPorts(EmbeddedDeliveries(kassa.delivery)),
            settings = SettingsPorts(
                coreSettings = EmbeddedSettings(kassa.settings, KassaSource.directory(this).path),
                workplace = PreferenceChoices(preferences)
            ),
            print = PrintPorts(SystemDialogPrintOut(screen), preferences.printing),
            update = UpdatePorts(releases = updates, updateMemory = updates),
            debug = DebugPorts(AppLogBook(DocumentFiles(screen))),
            analytics = analyticsPorts(cabinet, preferences),
            cabinet = cabinet,
            setup = SetupPorts(memory = preferences, cabinet = CabinetSetup(cabinet))
        )
    }

    /**
     * Аналитика — из того же кабинета, что и его разделы: доступ вошедшего
     * модуль кабинета наружу не отдаёт. Карты на Android пока нет.
     */
    private fun analyticsPorts(cabinet: RemoteCabinet, preferences: Preferences) = AnalyticsPorts(
        cabinet = CabinetAnalytics(cabinet.bfd, AppJournal(LogSource.Cabinet)),
        map = MapPorts(MapsNotOnAndroid(), WorkplaceMapMemory(preferences))
    )
}

private const val TAG = "Superkassa"
