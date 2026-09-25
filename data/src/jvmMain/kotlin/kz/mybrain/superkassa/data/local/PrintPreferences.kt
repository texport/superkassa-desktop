package kz.mybrain.superkassa.data.local

import kotlinx.io.files.Path
import kz.mybrain.superkassa.domain.print.model.PrintKind
import kz.mybrain.superkassa.domain.print.port.PrintChoices
import kz.mybrain.superkassa.domain.print.port.PrintChoices.Companion.MAX_COPIES

/**
 * Что помнит рабочее место о печати: принтер, вид формы и число копий.
 *
 * У кассы свой принтер — чековый, а не тот, на котором в конторе печатают
 * договоры. Выбор хранится по кассе: за одним компьютером их бывает две.
 */
class PrintPreferences(private val directory: Path) : PrintChoices {

    override fun printer(kkmId: String): String? = readSetting(printerFile(kkmId))

    override fun choosePrinter(kkmId: String, name: String?) = writeSetting(printerFile(kkmId), name)

    /** В каком виде сохранять печатную форму: `PNG`, `PDF` или `HTML`. */
    override fun kind(): PrintKind =
        PrintKind.entries.firstOrNull { it.name == readSetting(kindFile) } ?: PrintKind.Pdf

    override fun chooseKind(kind: PrintKind) = writeSetting(kindFile, kind.name)

    /**
     * Сколько копий печатать.
     *
     * Второй экземпляр чека нужен там, где его подшивают: на возвратах
     * и на корпоративных продажах. Больше трёх не бывает — это чек,
     * а не тираж.
     */
    override var copies: Int
        get() = readSetting(copiesFile)?.toIntOrNull()?.coerceIn(1, MAX_COPIES) ?: 1
        set(value) = writeSetting(copiesFile, value.coerceIn(1, MAX_COPIES).toString())

    private fun printerFile(kkmId: String) = Path(directory, "printers", kkmId)

    private val copiesFile = Path(directory, "print-copies")

    private val kindFile = Path(directory, "print-kind")
}
