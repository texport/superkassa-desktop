package kz.mybrain.superkassa.domain.print.port

import kz.mybrain.superkassa.domain.print.model.Kept
import kz.mybrain.superkassa.domain.print.model.PrintKind
import kz.mybrain.superkassa.domain.print.model.Printed

/**
 * Принтер и диск для проверок: принтеры те, что дали, ленту не режет.
 *
 * @property printed что ушло на принтер: лента, принтер, ширина и копии.
 * @property kept что сохранено в файл и под каким именем.
 */
class FakePrintOut(var names: List<String> = listOf("Чековый у кассы"), var accepts: Boolean = true) : PrintOut {
    data class Job(val tape: ByteArray, val printer: String?, val widthMm: Int, val copies: Int)

    val printed = mutableListOf<Job>()
    val kept = mutableListOf<Pair<String, ByteArray>>()

    override suspend fun printers(): List<String> = names

    override suspend fun tape(png: ByteArray): ByteArray = png

    override suspend fun print(tape: ByteArray, printer: String?, widthMm: Int, copies: Int): Printed {
        printed += Job(tape, printer, widthMm, copies)
        return if (accepts) Printed.Sent else Printed.Refused
    }

    override suspend fun keep(bytes: ByteArray, name: String, title: String): Kept {
        kept += name to bytes
        return Kept.Saved(name)
    }
}

/** Выбор печати без диска. */
class MemoryPrintChoices(override var copies: Int = 1, private var kind: PrintKind = PrintKind.Pdf) : PrintChoices {
    private val printers = mutableMapOf<String, String?>()

    override fun printer(kkmId: String): String? = printers[kkmId]

    override fun choosePrinter(kkmId: String, name: String?) {
        printers[kkmId] = name
    }

    override fun kind(): PrintKind = kind

    override fun chooseKind(kind: PrintKind) {
        this.kind = kind
    }
}
