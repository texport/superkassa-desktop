package kz.mybrain.superkassa.domain.settings.port

import io.github.texport.superkassa.core.domain.api.exception.SettingsFrozenException
import io.github.texport.superkassa.core.domain.api.model.settings.CoreMode
import io.github.texport.superkassa.core.domain.api.model.settings.CoreSettings
import io.github.texport.superkassa.core.domain.api.model.settings.StorageSettings
import io.github.texport.superkassa.core.string.api.TrilingualMessage
import kotlinx.coroutines.flow.update
import kz.mybrain.superkassa.domain.debug.port.DebugPorts
import kz.mybrain.superkassa.domain.debug.port.LogBook
import kz.mybrain.superkassa.domain.debug.port.MemoryLogBook
import kz.mybrain.superkassa.domain.map.model.MapProvider
import kz.mybrain.superkassa.domain.map.model.TileGrid
import kz.mybrain.superkassa.domain.print.port.FakePrintOut
import kz.mybrain.superkassa.domain.print.port.MemoryPrintChoices
import kz.mybrain.superkassa.domain.print.port.PrintChoices
import kz.mybrain.superkassa.domain.print.port.PrintOut
import kz.mybrain.superkassa.domain.print.port.PrintPorts
import kz.mybrain.superkassa.domain.update.port.FakeReleases
import kz.mybrain.superkassa.domain.update.port.MemoryUpdates
import kz.mybrain.superkassa.domain.update.port.Releases
import kz.mybrain.superkassa.domain.update.port.UpdateMemory
import kz.mybrain.superkassa.domain.update.port.UpdatePorts
import kz.mybrain.superkassa.domain.workplace.model.MapServices
import kz.mybrain.superkassa.domain.workplace.port.WorkplaceChoices
import kz.mybrain.superkassa.kassa.MemoryWorkplace

/**
 * Порты машины для проверок: в памяти, без диска, сети и окон.
 *
 * Одним набором — настройки, печать, выпуски и журнал, — как их
 * раздаёт точка сборки по областям; проверка, которой нужен свой порт,
 * подменяет его копией набора.
 */
fun settingsPorts() = MachinePorts(
    logBook = MemoryLogBook(),
    releases = FakeReleases(),
    updateMemory = MemoryUpdates(),
    printOut = FakePrintOut(),
    printChoices = MemoryPrintChoices(),
    coreSettings = MemoryCoreSettings(),
    workplace = MemoryChoices()
)

/** Порты машины одним набором; по областям их раскладывают [settings], [print], [update] и [debug]. */
data class MachinePorts(
    val logBook: LogBook,
    val releases: Releases,
    val updateMemory: UpdateMemory,
    val printOut: PrintOut,
    val printChoices: PrintChoices,
    val coreSettings: CoreSettingsStore,
    val workplace: WorkplaceChoices
) {
    val settings: SettingsPorts get() = SettingsPorts(coreSettings, workplace)
    val print: PrintPorts get() = PrintPorts(printOut, printChoices)
    val update: UpdatePorts get() = UpdatePorts(releases, updateMemory)
    val debug: DebugPorts get() = DebugPorts(logBook)
}

/**
 * Настройки кассы в памяти: правку принимает, пока она не закрыта.
 *
 * @property saves сколько раз настройки сохраняли.
 */
open class MemoryCoreSettings(
    var settings: CoreSettings = CoreSettings(CoreMode.DESKTOP, STORAGE, allowChanges = true),
    override val directory: String? = DIRECTORY
) : CoreSettingsStore {
    var saves = 0
        private set

    override suspend fun read(): CoreSettings = settings

    override suspend fun save(settings: CoreSettings): CoreSettings {
        if (!this.settings.allowChanges) throw SettingsFrozenException(FROZEN)
        saves++
        this.settings = settings
        return settings
    }

    private companion object {
        val STORAGE = StorageSettings(engine = "SQLITE", jdbcUrl = "jdbc:sqlite:superkassa.db", password = "secret")
        val FROZEN = TrilingualMessage("заморожено", "бұғатталған", "frozen")

        /** Каталог данных, как его назвала бы точка сборки. */
        const val DIRECTORY = "/home/kassa/.superkassa/kassa"
    }
}

/**
 * Настройки машины в памяти.
 *
 * @property memory память рабочего места: отрасль и своё название кассы
 *   пишутся туда, откуда их читают продажа и окно.
 */
class MemoryChoices(
    override var cabinetUrl: String = "https://bfd-cabinet.ecc.kz",
    override var cabinetServer: String = "",
    override var maps: MapServices = MapServices(),
    override val publicMaps: MapServices = MapServices(tiles = "https://tile.openstreetmap.org"),
    val memory: MemoryWorkplace = MemoryWorkplace(),
    override val mapProviders: List<MapProvider> = listOf(
        MapProvider.OpenStreetMap,
        MapProvider(id = "yandex", name = "Яндекс", attribution = "© Яндекс", grid = TileGrid.EllipticalMercator)
    )
) : WorkplaceChoices {

    override fun chooseDomain(kkmId: String, code: String?) {
        if (code == null) memory.domains.remove(kkmId) else memory.domains[kkmId] = code
    }

    override fun rename(kkmId: String, name: String?) {
        if (name == null) memory.names.remove(kkmId) else memory.names[kkmId] = name
    }
}
