package kz.mybrain.superkassa.data.local.workplace

import kotlinx.io.files.Path

/**
 * Каким кассир оставил окно: язык, оформление, размер и свёрнутые части.
 *
 * Отдельный предмет от прочих настроек рабочего места: здесь нет ни одного
 * значения, от которого зависит фискальная работа. Это выбор глазами —
 * его делают один раз под свой монитор и свою привычку, и повторять
 * каждое утро одни и те же нажатия кассир не должен.
 */
class ViewPreferences(private val directory: Path) {

    /** Язык интерфейса, выбранный кассиром. */
    var language: String?
        get() = readSetting(languageFile)
        set(value) = writeSetting(languageFile, value)

    /** Светлая или тёмная касса, выбранная кассиром. */
    var appearance: String?
        get() = readSetting(appearanceFile)
        set(value) = writeSetting(appearanceFile, value)

    /** Основной тон кассы, выбранный кассиром. */
    var accent: String?
        get() = readSetting(accentFile)
        set(value) = writeSetting(accentFile, value)

    /** Шрифт кассы, выбранный кассиром. */
    var typeface: String?
        get() = readSetting(typefaceFile)
        set(value) = writeSetting(typefaceFile, value)

    /** Размер шрифта кассы, выбранный кассиром. */
    var textScale: String?
        get() = readSetting(textScaleFile)
        set(value) = writeSetting(textScaleFile, value)

    /**
     * Размер окна кассы.
     *
     * Кассир растягивает окно под свой экран один раз, а не каждое утро:
     * на кассовом столе монитор не меняется. Хранится «ширина×высота»
     * одной строкой — разбирать нечего, а испорченная строка просто
     * не применяется.
     */
    var windowSize: Pair<Int, Int>?
        get() = readSetting(windowFile)?.split(SIZE_SEPARATOR)?.let { parts ->
            val width = parts.getOrNull(0)?.toIntOrNull()?.takeIf { it > 0 }
            val height = parts.getOrNull(1)?.toIntOrNull()?.takeIf { it > 0 }
            if (width == null || height == null) null else width to height
        }
        set(value) = writeSetting(
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
        get() = readSetting(panelsFile)?.split(LIST_SEPARATOR)?.filter { it.isNotBlank() }?.toSet().orEmpty()
        set(value) = writeSetting(panelsFile, value.joinToString(LIST_SEPARATOR).ifEmpty { null })

    /**
     * Свёрнут ли рельс разделов.
     *
     * На узком мониторе подписи разделов забирают ширину у чека, а кассир
     * и так знает их значки наизусть. Выбор держится рабочего места.
     */
    var railCollapsed: Boolean
        get() = readSetting(railFile) == COLLAPSED
        set(value) = writeSetting(railFile, if (value) COLLAPSED else null)

    /**
     * Свёрнута ли колонка торговых точек в кабинете.
     *
     * Владелец работает то со списком точек, то с одной кассой: свёрнутая
     * колонка отдаёт ширину карточке кассы. Помнится рабочим местом так
     * же, как свёрнутый рельс разделов.
     */
    var placesCollapsed: Boolean
        get() = readSetting(placesFile) == COLLAPSED
        set(value) = writeSetting(placesFile, if (value) COLLAPSED else null)

    /**
     * Свёрнута ли карточка кассы под картой касс.
     *
     * Карте нужна высота, а карточка нужна не всегда: владелец, который
     * ищет кассы глазами, сворачивает её и не должен сворачивать снова
     * при каждом открытии раздела.
     */
    var mapCardCollapsed: Boolean
        get() = readSetting(mapCardFile) == COLLAPSED
        set(value) = writeSetting(mapCardFile, if (value) COLLAPSED else null)

    /**
     * Свёрнута ли легенда карты касс.
     *
     * Что значат цвета кружков, читают один раз: дальше легенда только
     * занимает угол карты. Помнится так же, как карточка под картой.
     */
    var mapLegendCollapsed: Boolean
        get() = readSetting(mapLegendFile) == COLLAPSED
        set(value) = writeSetting(mapLegendFile, if (value) COLLAPSED else null)

    /**
     * Свёрнут ли счёт касс в окне карты: как и легенду, его читают
     * не всё время, а угол карты он занимает всегда.
     */
    var mapTallyCollapsed: Boolean
        get() = readSetting(mapTallyFile) == COLLAPSED
        set(value) = writeSetting(mapTallyFile, if (value) COLLAPSED else null)

    private val languageFile = Path(directory, "language")

    private val appearanceFile = Path(directory, "appearance")

    private val accentFile = Path(directory, "accent")

    private val typefaceFile = Path(directory, "typeface")

    private val textScaleFile = Path(directory, "textscale")

    private val windowFile = Path(directory, "window")

    private val panelsFile = Path(directory, "panels")

    private val railFile = Path(directory, "rail")

    private val placesFile = Path(directory, "places")

    private val mapCardFile = Path(directory, "map-card")

    private val mapLegendFile = Path(directory, "map-legend")

    private val mapTallyFile = Path(directory, "map-tally")

    companion object {
        /** Ширина и высота разделены крестиком: строка читаема глазами. */
        const val SIZE_SEPARATOR = "x"

        /** Разделитель перечня в однострочном файле настройки. */
        const val LIST_SEPARATOR = ","

        /** Отметка свёрнутого рельса: файла с другим содержимым не бывает. */
        const val COLLAPSED = "collapsed"
    }
}
