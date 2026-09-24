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
import kz.mybrain.superkassa.domain.print.model.Kept
import kz.mybrain.superkassa.domain.print.model.PrintKind
import kz.mybrain.superkassa.domain.print.port.PrintChoices
import kz.mybrain.superkassa.domain.print.port.PrintOut
import kz.mybrain.superkassa.domain.settings.port.CoreSettingsStore
import kz.mybrain.superkassa.domain.update.model.Fetched
import kz.mybrain.superkassa.domain.update.model.Installer
import kz.mybrain.superkassa.domain.update.model.ReleaseAnswer
import kz.mybrain.superkassa.domain.update.port.Releases
import kz.mybrain.superkassa.domain.update.port.UpdateMemory
import kz.mybrain.superkassa.domain.workplace.model.MapServices
import kz.mybrain.superkassa.domain.workplace.port.WorkplaceChoices
import kz.mybrain.superkassa.kassa.MemoryWorkplace
import kotlin.time.Instant

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
 * Служба выпусков для проверок: отвечает тем, что ей дали.
 *
 * @property fetched чем кончится скачивание установщика.
 * @property opened что открыто системой: скачанный файл или страница выпуска.
 */
class FakeReleases(
    var answer: ReleaseAnswer = ReleaseAnswer.Unreachable("no network in checks"),
    var fetched: Fetched = Fetched.Verified("/downloads/Superkassa-1.0.3.msi")
) : Releases {
    val opened = mutableListOf<String>()

    /** Какие установщики скачивали. */
    val downloaded = mutableListOf<Installer>()

    /** Сколько раз службу спросили. */
    var asked = 0
        private set

    override suspend fun latest(): ReleaseAnswer {
        asked++
        return answer
    }

    override suspend fun fetch(installer: Installer): Fetched {
        downloaded += installer
        return fetched
    }

    override fun openFile(file: String): Boolean = opened.add(file)

    override fun openPage(url: String): Boolean = opened.add(url)
}

/** Память о проверке выпусков без диска. */
class MemoryUpdates(override var automatic: Boolean = true, override var lastChecked: Instant? = null) : UpdateMemory

/**
 * Принтер и диск для проверок: принтеры те, что дали, ленту не режет.
 *
 * @property printed что ушло на принтер: лента, принтер, ширина и копии.
 * @property kept что сохранено в файл и под каким именем.
 */
class FakePrintOut(var names: List<String> = listOf("Чековый у кассы"), var accepts: Boolean = true) : PrintOut {
    data class Job(val tape: ByteArray, val printer: String?, val widthMm: Int, val copies: Int)

    val printed = mutableListOf<Job>()
    val kept = mutableListOf<Pair<String, ByteArray>>()

    override suspend fun printers(): List<String> = names

    override suspend fun tape(png: ByteArray): ByteArray = png

    override suspend fun print(tape: ByteArray, printer: String?, widthMm: Int, copies: Int): Boolean {
        printed += Job(tape, printer, widthMm, copies)
        return accepts
    }

    override suspend fun keep(bytes: ByteArray, name: String, title: String): Kept {
        kept += name to bytes
        return Kept.Saved(name)
    }
}

/** Выбор печати без диска. */
class MemoryPrintChoices(override var copies: Int = 1, private var kind: PrintKind = PrintKind.Pdf) : PrintChoices {
    private val printers = mutableMapOf<String, String?>()

    override fun printer(kkmId: String): String? = printers[kkmId]

    override fun choosePrinter(kkmId: String, name: String?) {
        printers[kkmId] = name
    }

    override fun kind(): PrintKind = kind

    override fun chooseKind(kind: PrintKind) {
        this.kind = kind
    }
}

/**
 * Настройки кассы в памяти: правку принимает, пока она не закрыта.
 *
 * @property saves сколько раз настройки сохраняли.
 */
open class MemoryCoreSettings(
    var settings: CoreSettings = CoreSettings(CoreMode.DESKTOP, STORAGE, allowChanges = true)
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
