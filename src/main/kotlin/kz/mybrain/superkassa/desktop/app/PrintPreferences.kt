package kz.mybrain.superkassa.desktop.app

import kz.mybrain.superkassa.desktop.server.PrintKind
import java.io.File

/**
 * Что помнит рабочее место о печати: принтер, вид формы и число копий.
 *
 * У кассы свой принтер — чековый, а не тот, на котором в конторе печатают
 * договоры. Выбор хранится по кассе: за одним компьютером их бывает две.
 */
class PrintPreferences(private val directory: File?) {

    fun printer(kkmId: String): String? = readSetting(printerFile(kkmId))

    fun choosePrinter(kkmId: String, name: String?) = writeSetting(printerFile(kkmId), name)

    /** В каком виде сохранять печатную форму: `PNG`, `PDF` или `HTML`. */
    fun kind(): PrintKind =
        PrintKind.entries.firstOrNull { it.name == readSetting(kindFile) } ?: PrintKind.Pdf

    fun chooseKind(kind: PrintKind) = writeSetting(kindFile, kind.name)

    /**
     * Сколько копий печатать.
     *
     * Второй экземпляр чека нужен там, где его подшивают: на возвратах
     * и на корпоративных продажах. Больше трёх не бывает — это чек,
     * а не тираж.
     */
    var copies: Int
        get() = readSetting(copiesFile)?.toIntOrNull()?.coerceIn(1, MAX_COPIES) ?: 1
        set(value) = writeSetting(copiesFile, value.coerceIn(1, MAX_COPIES).toString())

    private fun printerFile(kkmId: String) = File(directory, "printers/$kkmId")

    private val copiesFile = File(directory, "print-copies")

    private val kindFile = File(directory, "print-kind")

    companion object {
        /** Больше трёх копий чека не печатают: это чек, а не тираж. */
        const val MAX_COPIES = 3
    }
}
