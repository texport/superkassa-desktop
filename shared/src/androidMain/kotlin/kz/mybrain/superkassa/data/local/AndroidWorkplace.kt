package kz.mybrain.superkassa.data.local

import android.content.Context
import android.content.SharedPreferences
import kz.mybrain.superkassa.domain.workplace.WorkplaceMemory

/**
 * Память рабочего места на Android: настройки приложения, а не файлы.
 *
 * Пина здесь нет: он живёт только в памяти, как и на настольной кассе.
 */
class AndroidWorkplace(context: Context) : WorkplaceMemory {
    private val store: SharedPreferences = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    override var rememberedKkmId: String?
        get() = store.getString(REMEMBERED, null)
        set(value) {
            store.edit().putString(REMEMBERED, value).apply()
        }

    override fun localName(kkmId: String): String? = store.getString(NAME_PREFIX + kkmId, null)

    private companion object {
        const val FILE = "workplace"
        const val REMEMBERED = "kkm.remembered"
        const val NAME_PREFIX = "kkm.name."
    }
}
