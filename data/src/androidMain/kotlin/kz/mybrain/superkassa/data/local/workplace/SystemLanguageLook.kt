package kz.mybrain.superkassa.data.local.workplace

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.annotation.RequiresApi
import kz.mybrain.superkassa.domain.workplace.model.LookChoice
import kz.mybrain.superkassa.domain.workplace.port.LookMemory

/**
 * Вид окна на Android: язык — системный выбор языка приложения, остальное —
 * файлы рабочего места, как на компьютере.
 *
 * Android 13+ хранит язык приложения сам и даёт выбрать его в настройках
 * системы; выбор из кассы пишется туда же, и два места выбора не
 * расходятся. Сменённый язык система применяет, пересоздавая активность,
 * и надписи берутся заново. До Android 13 такого выбора у системы нет —
 * язык помнят файлы рабочего места.
 *
 * @param files вид окна в файлах рабочего места.
 */
class SystemLanguageLook(private val context: Context, private val files: LookMemory) : LookMemory {

    override var look: LookChoice
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            files.look.copy(language = SystemLocales.read(context))
        } else {
            files.look
        }
        set(value) {
            val changed = value.language != look.language
            if (changed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                SystemLocales.write(context, value.language)
            }
            files.look = value
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
