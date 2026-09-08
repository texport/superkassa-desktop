package kz.mybrain.superkassa.desktop.app

import kz.mybrain.superkassa.desktop.server.PrintKind
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetClient
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * То, что касса помнит между запусками.
 *
 * Здесь хранится только выбор кассы: на одном рабочем месте он не меняется
 * месяцами, и заставлять кассира искать свою кассу в списке каждое утро —
 * лишняя работа. Пин не хранится ни здесь, ни где-либо ещё на диске: он даёт
 * право на фискальные команды и живёт только в памяти запущенного приложения.
 *
 * Запись идёт во временный файл и переименованием поверх: прямая запись
 * сначала обрезает файл, и обрыв на этом месте оставлял огрызок значения —
 * касса переставала узнаваться, а причину было не видно.
 */
class Preferences(private val file: File = defaultFile()) {

    /** Касса, выбранная в прошлый раз. */
    var defaultKkmId: String?
        get() = read(file)
        set(value) = write(file, value)

    /** Язык интерфейса, выбранный кассиром. */
    var language: String?
        get() = read(languageFile)
        set(value) = write(languageFile, value)

    /** Светлая или тёмная касса, выбранная кассиром. */
    var appearance: String?
        get() = read(appearanceFile)
        set(value) = write(appearanceFile, value)

    /**
     * Размер окна кассы.
     *
     * Кассир растягивает окно под свой экран один раз, а не каждое утро:
     * на кассовом столе монитор не меняется. Хранится «ширина×высота»
     * одной строкой — разбирать нечего, а испорченная строка просто
     * не применяется.
     */
    var windowSize: Pair<Int, Int>?
        get() = read(windowFile)?.split(SIZE_SEPARATOR)?.let { parts ->
            val width = parts.getOrNull(0)?.toIntOrNull()?.takeIf { it > 0 }
            val height = parts.getOrNull(1)?.toIntOrNull()?.takeIf { it > 0 }
            if (width == null || height == null) null else width to height
        }
        set(value) = write(
            windowFile,
            value?.let { (width, height) -> "$width$SIZE_SEPARATOR$height" }
        )

    /**
     * Свёрнуты ли разделы кассовой колонки при открытии продажи.
     *
     * Кассир распоряжается высотой колонки сам, но каждое утро повторять
     * одни и те же три нажатия не должен: на одном рабочем месте порядок
     * работы один и тот же. Хранится перечень свёрнутых разделов одной
     * строкой; незнакомое имя просто не применяется.
     */
    var collapsedPanels: Set<String>
        get() = read(panelsFile)?.split(LIST_SEPARATOR)?.filter { it.isNotBlank() }?.toSet().orEmpty()
        set(value) = write(panelsFile, value.joinToString(LIST_SEPARATOR).ifEmpty { null })

    private val panelsFile = File(file.parentFile, "panels")

    /**
     * Свёрнут ли рельс разделов.
     *
     * На узком мониторе подписи разделов забирают ширину у чека, а кассир
     * и так знает их значки наизусть. Выбор держится рабочего места.
     */
    var railCollapsed: Boolean
        get() = read(railFile) == COLLAPSED
        set(value) = write(railFile, if (value) COLLAPSED else null)

    private val railFile = File(file.parentFile, "rail")

    /**
     * Адрес личного кабинета ОФД.
     *
     * Кабинет — отдельная служба: на этой машине он занимает соседний порт,
     * на стенде стоит своим адресом. Зашитый адрес заставил бы пересобирать
     * приложение ради переезда службы.
     */
    var cabinetUrl: String
        get() = read(cabinetFile) ?: CabinetClient.DEFAULT_URL
        set(value) = write(cabinetFile, value.trim().takeIf { it.isNotBlank() })

    private val cabinetFile = File(file.parentFile, "cabinet")

    /**
     * Пройденное в мастере подключения кассы.
     *
     * По файлу на значение — как и остальные настройки рабочего места:
     * разбирать один файл со своим форматом ради четырёх строк дороже,
     * чем хранить их порознь.
     */
    fun setupValue(name: String): String? = read(setupFile(name))

    fun setupValue(name: String, value: String?) = write(setupFile(name), value)

    private fun setupFile(name: String) = File(file.parentFile, "setup/$name")

    /**
     * Принтер этой кассы на этом рабочем месте.
     *
     * У кассы свой принтер — чековый, а не тот, на котором в конторе печатают
     * договоры. Выбор хранится по кассе: за одним компьютером их бывает две.
     */
    fun printer(kkmId: String): String? = read(printerFile(kkmId))

    fun choosePrinter(kkmId: String, name: String?) = write(printerFile(kkmId), name)

    /** В каком виде сохранять печатную форму: `PNG`, `PDF` или `HTML`. */
    fun printKind(): PrintKind =
        PrintKind.entries.firstOrNull { it.name == read(printKindFile) } ?: PrintKind.Pdf

    fun choosePrintKind(kind: PrintKind) = write(printKindFile, kind.name)

    private fun printerFile(kkmId: String) = File(file.parentFile, "printers/$kkmId")

    /**
     * Сколько копий печатать.
     *
     * Второй экземпляр чека нужен там, где его подшивают: на возвратах
     * и на корпоративных продажах. Больше трёх не бывает — это чек,
     * а не тираж.
     */
    var printCopies: Int
        get() = read(copiesFile)?.toIntOrNull()?.coerceIn(1, MAX_COPIES) ?: 1
        set(value) = write(copiesFile, value.coerceIn(1, MAX_COPIES).toString())

    private val copiesFile = File(file.parentFile, "print-copies")

    private val printKindFile = File(file.parentFile, "print-kind")

    private val languageFile = File(file.parentFile, "language")

    private val windowFile = File(file.parentFile, "window")

    private val appearanceFile = File(file.parentFile, "appearance")

    /**
     * Своё название кассы на этом рабочем месте.
     *
     * Все сведения о кассе — регистрационный номер, организация, адрес —
     * приходят от ОФД и здесь не меняются. Переименовать можно только
     * для себя: «Касса у входа» понятнее номера, когда их в зале четыре.
     */
    fun localName(kkmId: String): String? = read(nameFile(kkmId))

    fun rename(kkmId: String, name: String?) = write(nameFile(kkmId), name)

    private fun nameFile(kkmId: String) = File(file.parentFile, "names/$kkmId")

    private fun read(file: File): String? = runCatching {
        file.takeIf { it.exists() }?.readText()?.trim()?.takeIf { it.isNotEmpty() }
    }.getOrNull()

    private fun write(file: File, value: String?) {
        runCatching {
            val directory = file.parentFile
            directory?.mkdirs()
            if (value.isNullOrBlank()) {
                file.delete()
                return@runCatching
            }
            val temporary = File.createTempFile("kkm", ".tmp", directory)
            temporary.writeText(value)
            Files.move(
                temporary.toPath(),
                file.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        }
    }

    companion object {
        /** Ширина и высота разделены крестиком: строка читаема глазами. */
        const val SIZE_SEPARATOR = "x"

        /** Больше трёх копий чека не печатают: это чек, а не тираж. */
        const val MAX_COPIES = 3

        /** Разделитель перечня в однострочном файле настройки. */
        const val LIST_SEPARATOR = ","

        /** Отметка свёрнутого рельса: файла с другим содержимым не бывает. */
        const val COLLAPSED = "collapsed"

        fun defaultFile(): File = File(System.getProperty("user.home"), ".superkassa/kkm")
    }
}
