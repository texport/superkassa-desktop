package kz.mybrain.superkassa.domain.print.usecase

import kz.mybrain.superkassa.domain.print.model.PrintKind
import kz.mybrain.superkassa.domain.print.port.PrintChoices
import kz.mybrain.superkassa.domain.print.port.PrintOut

/** Куда печатает касса: принтеры машины, принтер кассы, копии и вид файла. */
class ReadPrintTarget(private val printOut: PrintOut, private val choices: PrintChoices) {

    /**
     * @property printers принтеры этой машины; пусто — принтера нет вовсе.
     * @property printer принтер кассы; `null` — системный по умолчанию.
     */
    data class Target(val printers: List<String>, val printer: String?, val copies: Int, val kind: PrintKind)

    /** @param kkmId касса, чей принтер нужен; `null` — касса не выбрана. */
    suspend operator fun invoke(kkmId: String?): Target =
        Target(printOut.printers(), kkmId?.let(choices::printer), choices.copies, choices.kind())
}
