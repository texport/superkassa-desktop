package kz.mybrain.superkassa.data.print

import android.content.Context
import android.content.SharedPreferences
import kz.mybrain.superkassa.domain.print.model.PrintKind
import kz.mybrain.superkassa.domain.print.port.PrintChoices

/**
 * Выбор печати на Android — в настройках приложения.
 *
 * Принтер кассы, копии и вид файла переживают перезапуск, как и на
 * настольной кассе: выбранное однажды не задают заново каждое утро.
 */
class AndroidPrintChoices(context: Context) : PrintChoices {
    private val store: SharedPreferences = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    override fun printer(kkmId: String): String? = store.getString(PRINTER_PREFIX + kkmId, null)

    override fun choosePrinter(kkmId: String, name: String?) {
        store.edit().putString(PRINTER_PREFIX + kkmId, name).apply()
    }

    override var copies: Int
        get() = store.getInt(COPIES, 1).coerceIn(1, PrintChoices.MAX_COPIES)
        set(value) {
            store.edit().putInt(COPIES, value.coerceIn(1, PrintChoices.MAX_COPIES)).apply()
        }

    override fun kind(): PrintKind =
        PrintKind.entries.firstOrNull { it.name == store.getString(KIND, null) } ?: PrintKind.Pdf

    override fun chooseKind(kind: PrintKind) {
        store.edit().putString(KIND, kind.name).apply()
    }

    private companion object {
        const val FILE = "print"
        const val PRINTER_PREFIX = "printer."
        const val COPIES = "copies"
        const val KIND = "kind"
    }
}
