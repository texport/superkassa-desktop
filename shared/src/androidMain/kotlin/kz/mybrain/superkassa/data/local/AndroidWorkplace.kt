package kz.mybrain.superkassa.data.local

import android.content.Context
import android.content.SharedPreferences
import kz.mybrain.superkassa.domain.setup.port.SetupMemory
import kz.mybrain.superkassa.domain.workplace.model.LookChoice
import kz.mybrain.superkassa.domain.workplace.model.MapServices
import kz.mybrain.superkassa.domain.workplace.port.LookMemory
import kz.mybrain.superkassa.domain.workplace.port.WorkplaceMemory

/**
 * Память рабочего места на Android: настройки приложения, а не файлы.
 *
 * Пина здесь нет: он живёт только в памяти, как и на настольной кассе.
 * Токена кассы нет тоже: пройденное мастером подключения его не хранит.
 */
class AndroidWorkplace(context: Context) : WorkplaceMemory, LookMemory, SetupMemory {
    private val store: SharedPreferences = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    private val language = AppLanguage(context.applicationContext, store)

    override var rememberedKkmId: String?
        get() = store.getString(REMEMBERED, null)
        set(value) {
            store.edit().putString(REMEMBERED, value).apply()
        }

    override fun localName(kkmId: String): String? = store.getString(NAME_PREFIX + kkmId, null)

    override fun domain(kkmId: String): String? = store.getString(DOMAIN_PREFIX + kkmId, null)

    /** Отрасль кассы; `null` снимает выбор, и касса торгует. */
    fun chooseDomain(kkmId: String, code: String?) {
        store.edit().putString(DOMAIN_PREFIX + kkmId, code).apply()
    }

    /** Своё название кассы; `null` снимает его. */
    fun rename(kkmId: String, name: String?) {
        store.edit().putString(NAME_PREFIX + kkmId, name).apply()
    }

    /** Адрес личного кабинета БФД; не задан — пусто. */
    var cabinetUrl: String
        get() = store.getString(CABINET, null).orEmpty()
        set(value) {
            store.edit().putString(CABINET, value.trim().ifBlank { null }).apply()
        }

    /** Службы карты; пустое поле — общедоступная служба. */
    var maps: MapServices
        get() = MapServices(
            tiles = store.getString(MAP_TILES, null),
            search = store.getString(MAP_SEARCH, null),
            reverse = store.getString(MAP_REVERSE, null),
            location = store.getString(MAP_LOCATION, null)
        )
        set(value) {
            store.edit()
                .putString(MAP_TILES, value.tiles)
                .putString(MAP_SEARCH, value.search)
                .putString(MAP_REVERSE, value.reverse)
                .putString(MAP_LOCATION, value.location)
                .apply()
        }

    override var collapsedPanels: Set<String>
        get() = store.getStringSet(PANELS, null).orEmpty()
        set(value) {
            store.edit().putStringSet(PANELS, value).apply()
        }

    override fun setupValue(name: String): String? = store.getString(SETUP_PREFIX + name, null)

    override fun setupValue(name: String, value: String?) {
        store.edit().putString(SETUP_PREFIX + name, value).apply()
    }

    /** Вид окна; язык — системный выбор языка приложения ([AppLanguage]). */
    override var look: LookChoice
        get() = LookChoice(
            language = language.code,
            appearance = store.getString(APPEARANCE, null),
            accent = store.getString(ACCENT, null),
            typeface = store.getString(TYPEFACE, null),
            textScale = store.getString(TEXT_SCALE, null)
        )
        set(value) {
            language.code = value.language
            store.edit()
                .putString(APPEARANCE, value.appearance)
                .putString(ACCENT, value.accent)
                .putString(TYPEFACE, value.typeface)
                .putString(TEXT_SCALE, value.textScale)
                .apply()
        }

    private companion object {
        const val FILE = "workplace"
        const val REMEMBERED = "kkm.remembered"
        const val NAME_PREFIX = "kkm.name."
        const val DOMAIN_PREFIX = "kkm.domain."
        const val SETUP_PREFIX = "setup."
        const val PANELS = "sale.panels.collapsed"
        const val CABINET = "cabinet.url"
        const val MAP_TILES = "map.tiles"
        const val MAP_SEARCH = "map.search"
        const val MAP_REVERSE = "map.reverse"
        const val MAP_LOCATION = "map.location"
        const val APPEARANCE = "look.appearance"
        const val ACCENT = "look.accent"
        const val TYPEFACE = "look.typeface"
        const val TEXT_SCALE = "look.textScale"
    }
}
