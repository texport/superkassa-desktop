package kz.mybrain.superkassa.desktop.app

import kz.mybrain.superkassa.desktop.server.PrintKind
import java.io.File

/**
 * То, что касса помнит между запусками.
 *
 * Здесь не хранится ничего само по себе: рабочее место спрашивает настройку
 * одним именем, а за каждым предметом стоит свой хранитель — узел и касса
 * [NodePreferences], кабинет [CabinetPreferences], вид окна [ViewPreferences],
 * мастер подключения [SetupPreferences], согласие на определение места
 * [LocationPreferences], карта [MapPreferences], печать [PrintPreferences],
 * проверка обновлений [UpdatePreferences].
 * Так добавленная настройка ложится к своему предмету, а не в общую кучу.
 *
 * Пин не хранится ни здесь, ни где-либо ещё на диске: он даёт право
 * на фискальные команды и живёт только в памяти запущенного приложения.
 */
class Preferences(private val file: File = defaultFile()) {

    /** Узел, выбранная касса и её название на этом рабочем месте. */
    val node = NodePreferences(file)

    /** Адрес кабинета и личность для входа без ЭЦП. */
    val cabinet = CabinetPreferences(file.parentFile)

    /** Язык, оформление, размер окна и свёрнутые части. */
    val view = ViewPreferences(file.parentFile)

    /** Пройденное в мастере подключения кассы. */
    val setup = SetupPreferences(file.parentFile)

    /** Согласие владельца на определение места по адресу подключения. */
    val location = LocationPreferences(file.parentFile)

    /** Свои службы карты; пустые — общедоступные службы сообщества. */
    val maps = MapPreferences(file.parentFile)

    /** Настройки печати: принтер, вид формы и число копий. */
    val printing = PrintPreferences(file.parentFile)

    /** Проверять ли выпуски самой и когда проверяли в последний раз. */
    val updates = UpdatePreferences(file.parentFile)

    var defaultKkmId: String?
        get() = node.defaultKkmId
        set(value) {
            node.defaultKkmId = value
        }

    var nodeUrl: String
        get() = node.url
        set(value) {
            node.url = value
        }

    /** Вид отрасли этой кассы; пусто — торговля. */
    var domain: String?
        get() = node.domain
        set(value) {
            node.domain = value
        }

    fun localName(kkmId: String): String? = node.localName(kkmId)

    fun rename(kkmId: String, name: String?) = node.rename(kkmId, name)

    var cabinetUrl: String
        get() = cabinet.url
        set(value) {
            cabinet.url = value
        }

    var language: String?
        get() = view.language
        set(value) {
            view.language = value
        }

    var appearance: String?
        get() = view.appearance
        set(value) {
            view.appearance = value
        }

    var accent: String?
        get() = view.accent
        set(value) {
            view.accent = value
        }

    var typeface: String?
        get() = view.typeface
        set(value) {
            view.typeface = value
        }

    var textScale: String?
        get() = view.textScale
        set(value) {
            view.textScale = value
        }

    var windowSize: Pair<Int, Int>?
        get() = view.windowSize
        set(value) {
            view.windowSize = value
        }

    var collapsedPanels: Set<String>
        get() = view.collapsedPanels
        set(value) {
            view.collapsedPanels = value
        }

    var railCollapsed: Boolean
        get() = view.railCollapsed
        set(value) {
            view.railCollapsed = value
        }

    var placesCollapsed: Boolean
        get() = view.placesCollapsed
        set(value) {
            view.placesCollapsed = value
        }

    var mapCardCollapsed: Boolean
        get() = view.mapCardCollapsed
        set(value) {
            view.mapCardCollapsed = value
        }

    var mapLegendCollapsed: Boolean
        get() = view.mapLegendCollapsed
        set(value) {
            view.mapLegendCollapsed = value
        }

    var locationAllowed: Boolean?
        get() = location.allowed
        set(value) {
            location.allowed = value
        }

    fun setupValue(name: String): String? = setup.value(name)

    fun setupValue(name: String, value: String?) = setup.remember(name, value)

    fun printer(kkmId: String): String? = printing.printer(kkmId)

    fun choosePrinter(kkmId: String, name: String?) = printing.choosePrinter(kkmId, name)

    fun printKind(): PrintKind = printing.kind()

    fun choosePrintKind(kind: PrintKind) = printing.chooseKind(kind)

    var printCopies: Int
        get() = printing.copies
        set(value) {
            printing.copies = value
        }

    companion object {
        fun defaultFile(): File = File(System.getProperty("user.home"), ".superkassa/kkm")
    }
}
