package kz.mybrain.superkassa.data.local

import android.app.LocaleManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.LocaleList
import androidx.annotation.RequiresApi

/**
 * Язык приложения на Android — системный выбор языка приложения.
 *
 * Android 13+ хранит его сам и даёт выбрать в настройках системы; выбор
 * из кассы пишется туда же, и два места выбора не расходятся. Сменённый
 * язык система применяет, пересоздавая активность, и надписи берутся
 * заново. До Android 13 такого выбора у системы нет — язык помнят
 * настройки приложения.
 */
internal class AppLanguage(private val context: Context, private val store: SharedPreferences) {

    /** Код языка; `null` — как в системе. */
    var code: String?
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) SystemLocales.read(context) else stored()
        set(value) {
            if (value == code) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                SystemLocales.write(context, value)
            } else {
                store.edit().putString(LANGUAGE, value).apply()
            }
        }

    private fun stored(): String? = store.getString(LANGUAGE, null)

    private companion object {
        const val LANGUAGE = "look.language"
    }
}

/** Языки приложения, которые хранит сама система. */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private object SystemLocales {
    fun read(context: Context): String? =
        manager(context).applicationLocales.takeUnless { it.isEmpty }?.get(0)?.language

    fun write(context: Context, code: String?) {
        manager(context).applicationLocales = LocaleList.forLanguageTags(code.orEmpty())
    }

    private fun manager(context: Context): LocaleManager = context.getSystemService(LocaleManager::class.java)
}
