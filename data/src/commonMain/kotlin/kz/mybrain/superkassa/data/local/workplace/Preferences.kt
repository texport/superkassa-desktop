package kz.mybrain.superkassa.data.local.workplace

import kotlinx.io.files.Path
import kz.mybrain.superkassa.domain.setup.port.SetupMemory
import kz.mybrain.superkassa.domain.workplace.model.LookChoice
import kz.mybrain.superkassa.domain.workplace.port.LookMemory
import kz.mybrain.superkassa.domain.workplace.port.WorkplaceMemory

/**
 * То, что касса помнит между запусками.
 *
 * Здесь не хранится ничего само по себе: рабочее место спрашивает настройку
 * одним именем, а за каждым предметом стоит свой хранитель — касса
 * [KkmPreferences], кабинет [CabinetPreferences], вид окна [ViewPreferences],
 * мастер подключения [SetupPreferences], согласие на определение места
 * [LocationPreferences], карта [MapPreferences], печать [PrintPreferences],
 * проверка обновлений [UpdatePreferences].
 * Так добавленная настройка ложится к своему предмету, а не в общую кучу.
 *
 * Пин не хранится ни здесь, ни где-либо ещё на диске: он даёт право
 * на фискальные команды и живёт только в памяти запущенного приложения.
 *
 * @param directory каталог данных рабочего места: файлы настроек лежат в нём.
 */
class Preferences(directory: Path) : WorkplaceMemory, SetupMemory, LookMemory {

    /** Выбранная касса, её отрасль и название на этом рабочем месте. */
    val kkm = KkmPreferences(directory)

    /** Адрес кабинета и личность для входа без ЭЦП. */
    val cabinet = CabinetPreferences(directory)

    /** Язык, оформление, размер окна и свёрнутые части. */
    val view = ViewPreferences(directory)

    /** Пройденное в мастере подключения кассы. */
    val setup = SetupPreferences(directory)

    /** Согласие владельца на определение места по адресу подключения. */
    val location = LocationPreferences(directory)

    /** Свои службы карты; пустые — общедоступные службы сообщества. */
    val maps = MapPreferences(directory)

    /** Настройки печати: принтер, вид формы и число копий. */
    val printing = PrintPreferences(directory)

    /** Проверять ли выпуски самой и когда проверяли в последний раз. */
    val updates = UpdatePreferences(directory)

    override var rememberedKkmId: String?
        get() = kkm.defaultKkmId
        set(value) {
            kkm.defaultKkmId = value
        }

    /** Адрес прежнего узла кассы: нужен только переносу его данных. */
    val formerNodeAddress: String get() = kkm.formerNodeAddress

    /** Вид отрасли этой кассы; пусто — торговля. */
    override fun domain(kkmId: String): String? = kkm.domain(kkmId)

    fun chooseDomain(kkmId: String, code: String?) = kkm.chooseDomain(kkmId, code)

    override fun localName(kkmId: String): String? = kkm.localName(kkmId)

    fun rename(kkmId: String, name: String?) = kkm.rename(kkmId, name)

    var cabinetUrl: String
        get() = cabinet.url
        set(value) {
            cabinet.url = value
        }

    var cabinetServer: String
        get() = cabinet.server
        set(value) {
            cabinet.server = value
        }

    override var look: LookChoice
        get() = LookChoice(
            language = view.language,
            appearance = view.appearance,
            accent = view.accent,
            typeface = view.typeface,
            textScale = view.textScale,
            railCollapsed = view.railCollapsed,
            placesCollapsed = view.placesCollapsed
        )
        set(value) {
            view.language = value.language
            view.appearance = value.appearance
            view.accent = value.accent
            view.typeface = value.typeface
            view.textScale = value.textScale
            view.railCollapsed = value.railCollapsed
            view.placesCollapsed = value.placesCollapsed
        }

    var windowSize: Pair<Int, Int>?
        get() = view.windowSize
        set(value) {
            view.windowSize = value
        }

    override var collapsedPanels: Set<String>
        get() = view.collapsedPanels
        set(value) {
            view.collapsedPanels = value
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

    var mapListCollapsed: Boolean
        get() = view.mapListCollapsed
        set(value) {
            view.mapListCollapsed = value
        }

    var locationAllowed: Boolean?
        get() = location.allowed
        set(value) {
            location.allowed = value
        }

    override fun setupValue(name: String): String? = setup.value(name)

    override fun setupValue(name: String, value: String?) = setup.remember(name, value)
}
