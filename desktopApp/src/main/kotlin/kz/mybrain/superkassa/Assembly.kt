package kz.mybrain.superkassa

import io.github.texport.superkassa.embedded.api.Superkassa
import io.github.texport.superkassa.embedded.api.SuperkassaPlatform
import io.github.texport.superkassa.embedded.api.createSuperkassa
import io.github.texport.superkassa.importnode.api.NodeImportResult
import kz.mybrain.superkassa.data.analytics.CabinetAnalytics
import kz.mybrain.superkassa.data.cabinet.DeveloperEntry
import kz.mybrain.superkassa.data.cabinet.RemoteCabinet
import kz.mybrain.superkassa.data.cabinet.setup.CabinetSetup
import kz.mybrain.superkassa.data.eds.NcaSigner
import kz.mybrain.superkassa.data.kassa.EmbeddedKassa
import kz.mybrain.superkassa.data.kassa.NodeDataMove
import kz.mybrain.superkassa.data.kassa.delivery.EmbeddedDeliveries
import kz.mybrain.superkassa.data.kassa.delivery.EmbeddedDeliverySetup
import kz.mybrain.superkassa.data.kassa.settings.EmbeddedSettings
import kz.mybrain.superkassa.data.local.DataHome
import kz.mybrain.superkassa.data.local.DialogFiles
import kz.mybrain.superkassa.data.local.PreferenceChoices
import kz.mybrain.superkassa.data.local.Preferences
import kz.mybrain.superkassa.data.log.AppJournal
import kz.mybrain.superkassa.data.log.AppLog
import kz.mybrain.superkassa.data.log.AppLogBook
import kz.mybrain.superkassa.data.log.LogLevel
import kz.mybrain.superkassa.data.log.LogSource
import kz.mybrain.superkassa.data.map.DiskTiles
import kz.mybrain.superkassa.data.map.MacLocation
import kz.mybrain.superkassa.data.map.MapAddresses
import kz.mybrain.superkassa.data.map.OpenStreetMaps
import kz.mybrain.superkassa.data.map.WorkplaceMapMemory
import kz.mybrain.superkassa.data.map.mapJournal
import kz.mybrain.superkassa.data.print.SystemPrintOut
import kz.mybrain.superkassa.data.releases.GithubUpdates
import kz.mybrain.superkassa.domain.journal.port.JournalPorts
import kz.mybrain.superkassa.domain.kassa.model.StartProblem
import kz.mybrain.superkassa.domain.kassa.model.StartRefusal
import kz.mybrain.superkassa.domain.kassa.port.KassaPorts
import kz.mybrain.superkassa.domain.setup.port.SetupPorts
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.domain.workplace.model.WorkplaceLook
import kz.mybrain.superkassa.integrations.bfdcabinet.BfdCabinet
import kz.mybrain.superkassa.integrations.maps.OpenMaps
import kz.mybrain.superkassa.presentation.analytics.AnalyticsPorts
import kz.mybrain.superkassa.presentation.common.mapview.MapPorts
import kz.mybrain.superkassa.presentation.common.message.Notices
import kz.mybrain.superkassa.presentation.common.model.Talk
import kz.mybrain.superkassa.presentation.common.strings.workplaceLanguage
import kz.mybrain.superkassa.presentation.settings.SettingsPorts
import kz.mybrain.superkassa.presentation.shell.AppContainer
import kz.mybrain.superkassa.presentation.shell.AreaPorts
import java.io.File

/**
 * Точка сборки настольной кассы — единственное место, знающее все три слоя.
 *
 * Здесь реализации `data` становятся портами `domain` и уходят экранам
 * в [AppContainer]. Область, заведшая новый порт, дописывает его строку
 * в [assemble] — под своей областью, как и в самом контейнере.
 */

/** Каталог плиток карты в каталоге данных рабочего места. */
private const val TILES = "tiles"

/** Собирает зависимости экранов: порты `domain` из реализаций `data`. */
internal fun assemble(kassa: Superkassa, preferences: Preferences, look: WorkplaceLook): AppContainer {
    val language = { workplaceLanguage(look.state.value.language) }
    // Кабинет и мастер заведения кассы: один кабинет на приложение и подпись владельца.
    val cabinet = cabinet(preferences) { language().code }
    return AppContainer(
        // Общее.
        kassa = EmbeddedKassa(kassa.api),
        signIn = SignIn(),
        memory = preferences,
        look = look,
        talk = Talk(Notices(), AppJournal(), language),
        areas = AreaPorts(
            kassa = KassaPorts(EmbeddedDeliverySetup(kassa.settings)),
            journal = JournalPorts(EmbeddedDeliveries(kassa.delivery)),
            settings = settingsPorts(kassa, preferences),
            analytics = analyticsPorts(cabinet.bfd, preferences) { language().code },
            cabinet = cabinet,
            setup = SetupPorts(memory = preferences, cabinet = CabinetSetup(cabinet))
        )
    )
}

/**
 * Кабинет по адресу рабочего места с подписью NCALayer на языке кассира.
 *
 * Вход разработчика заголовками — только явной настройкой машины, см. [DeveloperEntry].
 */
private fun cabinet(preferences: Preferences, language: () -> String): RemoteCabinet = RemoteCabinet.open(
    preferences.cabinetUrl,
    NcaSigner(locale = language),
    DialogFiles(),
    AppJournal(LogSource.Cabinet),
    DeveloperEntry.fromEnvironment()
)

/** Порты настроек, печати, обновления и журнала отладки — из реализаций настольной кассы. */
private fun settingsPorts(kassa: Superkassa, preferences: Preferences) = SettingsPorts(
    logBook = AppLogBook(),
    releases = GithubUpdates(),
    updateMemory = preferences.updates,
    printOut = SystemPrintOut(),
    printChoices = preferences.printing,
    coreSettings = EmbeddedSettings(kassa.settings, DataHome.kassa().path),
    workplace = PreferenceChoices(preferences)
)

/**
 * Аналитика кабинета и службы карт; карта говорит на языке кассира.
 *
 * Аналитика ходит в тот же экземпляр кабинета, что и его разделы: доступ
 * вошедшего модуль кабинета наружу не отдаёт, и второй экземпляр получил
 * бы отказ.
 */
private fun analyticsPorts(bfd: BfdCabinet, preferences: Preferences, language: () -> String): AnalyticsPorts {
    val maps = OpenMaps(
        services = MapAddresses(preferences.maps)::services,
        tiles = DiskTiles(File(DataHome.directory(), TILES).path),
        journal = mapJournal(AppJournal(LogSource.App))
    )
    return AnalyticsPorts(
        cabinet = CabinetAnalytics(bfd, AppJournal(LogSource.Cabinet)),
        map = MapPorts(OpenStreetMaps(maps, language, MacLocation::locate), WorkplaceMapMemory(preferences))
    )
}

/** Чем кончился запуск: касса открыта — или почему нет. */
internal sealed interface KassaStart {
    class Opened(val kassa: Superkassa) : KassaStart

    class Refused(val problem: StartProblem) : KassaStart
}

/**
 * Переносит данные прежнего узла и поднимает кассу на каталоге данных.
 *
 * Перенос идёт до кассы при каждом запуске: первый переносит, следующие
 * отвечают, что всё уже перенесено. Несостоявшийся перенос останавливает
 * запуск: пустая касса выглядела бы потерянными сменами и толкала бы
 * к повторной регистрации. Второй экземпляр на том же каталоге касса
 * не откроет — и об этом тоже говорится экраном.
 */
internal fun startKassa(preferences: Preferences): KassaStart {
    val directory = DataHome.kassa()
    val moved = NodeDataMove.run(DataHome.directory(), directory, preferences.formerNodeAddress)
    moved.exceptionOrNull()?.let { return refused("node data not moved", it, NodeDataMove.problemOf(it)) }
    AppLog.state("node data: ${moved.getOrNull()?.let(::outcomeOf)}")
    return runCatching { createSuperkassa(SuperkassaPlatform(directory.path), EmbeddedKassa.config()) }
        .fold(
            onSuccess = { KassaStart.Opened(it) },
            onFailure = { refused("kassa did not open", it, StartProblem(StartRefusal.KassaNotOpened, directory.path)) }
        )
}

/** Итог переноса для журнала: без токенов и документов — только что случилось. */
private fun outcomeOf(result: NodeImportResult): String = when (result) {
    is NodeImportResult.Imported -> "imported ${result.report.kkms.size} cash registers"
    NodeImportResult.AlreadyImported -> "already imported"
    NodeImportResult.NoNodeData -> "no node data"
}

/** Отказ запуска: в журнал — что и почему, кассиру — экраном его словами. */
private fun refused(what: String, failure: Throwable, problem: StartProblem): KassaStart.Refused {
    AppLog.record(LogSource.Machine, LogLevel.Failure, "$what: ${failure::class.simpleName} ${problem.refusal}")
    return KassaStart.Refused(problem)
}
