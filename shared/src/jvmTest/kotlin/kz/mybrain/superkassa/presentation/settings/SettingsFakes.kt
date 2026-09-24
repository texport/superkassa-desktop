package kz.mybrain.superkassa.presentation.settings

import io.github.texport.superkassa.core.domain.api.exception.SettingsFrozenException
import io.github.texport.superkassa.core.domain.api.model.settings.CoreMode
import io.github.texport.superkassa.core.domain.api.model.settings.CoreSettings
import io.github.texport.superkassa.core.domain.api.model.settings.StorageSettings
import io.github.texport.superkassa.core.string.api.TrilingualMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kz.mybrain.superkassa.domain.debug.model.LogEntry
import kz.mybrain.superkassa.domain.debug.model.LogLevel
import kz.mybrain.superkassa.domain.debug.port.LogBook
import kz.mybrain.superkassa.domain.debug.port.LogBookState
import kz.mybrain.superkassa.domain.print.port.FakePrintOut
import kz.mybrain.superkassa.domain.print.port.MemoryPrintChoices
import kz.mybrain.superkassa.domain.settings.port.CoreSettingsStore
import kz.mybrain.superkassa.domain.update.port.FakeReleases
import kz.mybrain.superkassa.domain.update.port.MemoryUpdates
import kz.mybrain.superkassa.domain.workplace.model.MapServices
import kz.mybrain.superkassa.domain.workplace.port.WorkplaceChoices
import kz.mybrain.superkassa.kassa.MemoryWorkplace

/**
 * Порты области настроек для проверок: в памяти, без диска, сети и окон.
 *
 * Проверка, которой нужен свой порт, подменяет его копией набора.
 */
fun settingsPorts() = SettingsPorts(
    logBook = MemoryLogBook(),
    releases = FakeReleases(),
    updateMemory = MemoryUpdates(),
    printOut = FakePrintOut(),
    printChoices = MemoryPrintChoices(),
    coreSettings = MemoryCoreSettings(),
    workplace = MemoryChoices()
)

/**
 * Журнал для проверок: книга в памяти, без файла и без окна выбора.
 *
 * @property saved что просили сохранить в последний раз.
 */
class MemoryLogBook(initial: LogBookState = LogBookState()) : LogBook {
    override val state = MutableStateFlow(initial)

    var saved: List<LogEntry>? = null
        private set

    override fun chooseLevel(level: LogLevel) = state.update { it.copy(level = level) }

    override fun switchDebugMode(on: Boolean) = state.update { it.copy(debugMode = on) }

    override fun clear() = state.update { it.copy(entries = emptyList()) }

    override suspend fun save(lines: List<LogEntry>, title: String) {
        saved = lines
    }
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
    override var maps: MapServices = MapServices(),
    override val publicMaps: MapServices = MapServices(tiles = "https://tile.openstreetmap.org"),
    val memory: MemoryWorkplace = MemoryWorkplace()
) : WorkplaceChoices {

    override fun chooseDomain(kkmId: String, code: String?) {
        if (code == null) memory.domains.remove(kkmId) else memory.domains[kkmId] = code
    }

    override fun rename(kkmId: String, name: String?) {
        if (name == null) memory.names.remove(kkmId) else memory.names[kkmId] = name
    }
}
