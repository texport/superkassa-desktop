package kz.mybrain.superkassa.data.print

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kz.mybrain.superkassa.data.local.askWhereToSave
import kz.mybrain.superkassa.data.log.AppLog
import kz.mybrain.superkassa.domain.debug.model.LogSource
import kz.mybrain.superkassa.domain.print.model.Kept
import kz.mybrain.superkassa.domain.print.port.PrintOut

/**
 * Принтеры и диск этой машины.
 *
 * Работа с картинкой ленты и с принтером идёт в потоке ввода-вывода:
 * у ленты Z-отчёта срез полей — проход по двум десяткам миллионов точек,
 * около секунды работы. В потоке экрана касса на всё это время переставала
 * отзываться — кассир жал «Печать» и получал застывший экран.
 *
 * @param io где выполнять работу; проверкам подставляется свой диспетчер.
 */
class SystemPrintOut(private val io: CoroutineDispatcher = Dispatchers.IO) : PrintOut {

    override suspend fun printers(): List<String> = withContext(io) { Printing.printers() }

    override suspend fun tape(png: ByteArray): ByteArray = withContext(io) { Printing.trim(png) }

    override suspend fun print(tape: ByteArray, printer: String?, widthMm: Int, copies: Int): Boolean =
        withContext(io) {
            runCatching { Printing.print(tape, printer, widthMm, copies) }
                .onFailure { AppLog.warn(LogSource.App, "print job refused: ${it::class.simpleName}") }
                .getOrDefault(false)
        }

    override suspend fun keep(bytes: ByteArray, name: String, title: String): Kept = withContext(io) {
        val target = askWhereToSave(name, title) ?: return@withContext Kept.Cancelled
        Printing.save(bytes, target)
        Kept.Saved(target.name)
    }
}
