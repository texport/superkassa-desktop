package kz.mybrain.superkassa.data.print

import android.print.PrintJob
import android.print.PrintManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kz.mybrain.superkassa.data.local.ForegroundActivity
import kz.mybrain.superkassa.domain.print.model.Kept
import kz.mybrain.superkassa.domain.print.model.PrintRoute
import kz.mybrain.superkassa.domain.print.model.Printed
import kz.mybrain.superkassa.domain.print.port.PrintOut
import kotlin.coroutines.resume

/**
 * Печать на Android — системным диалогом печати.
 *
 * Принтеров по имени у приложения нет: диалог системы сам показывает
 * принтеры, подключённые службами печати, и «Сохранить как PDF», а в нём
 * же выбирают копии и бумагу. Форма уходит туда документом PDF, который
 * нарисовала касса. Сохранение в файл — системным окном «Сохранить»:
 * владелец выбирает папку и имя сам.
 *
 * Итог печати известен, когда диалог закрыт: задание ушло, отказано или
 * кассир передумал. Отмена отказом не называется.
 */
class SystemDialogPrintOut(private val screen: ForegroundActivity) : PrintOut {

    override val route: PrintRoute = PrintRoute.SystemDialog

    override suspend fun printers(): List<String> = emptyList()

    override suspend fun tape(png: ByteArray): ByteArray = png

    override suspend fun print(tape: ByteArray, printer: String?, widthMm: Int, copies: Int): Printed =
        withContext(Dispatchers.Main) {
            val activity = screen.activity ?: return@withContext Printed.Refused
            val manager = activity.getSystemService(PrintManager::class.java) ?: return@withContext Printed.Refused
            suspendCancellableCoroutine { answer ->
                var job: PrintJob? = null
                val adapter = PdfPrintAdapter(tape, JOB) {
                    if (answer.isActive) answer.resume(outcome(job))
                }
                job = manager.print(JOB, adapter, null)
            }
        }

    override suspend fun keep(bytes: ByteArray, name: String, title: String): Kept {
        val target = screen.createDocument(name, mimeOf(name)) ?: return Kept.Cancelled
        val written = withContext(Dispatchers.IO) {
            runCatching { screen.activity?.contentResolver?.openOutputStream(target)?.use { it.write(bytes) } }
                .getOrNull()
        }
        return if (written != null) Kept.Saved(name) else Kept.Unavailable
    }

    /** Чем кончилось задание, когда диалог закрыт. */
    private fun outcome(job: PrintJob?): Printed = when {
        job == null || job.isFailed -> Printed.Refused
        job.isCancelled -> Printed.Cancelled
        else -> Printed.Sent
    }

    /** Вид файла по его окончанию: система подписывает по нему окно и выбирает, чем открыть. */
    private fun mimeOf(name: String): String = when (name.substringAfterLast('.').lowercase()) {
        "pdf" -> "application/pdf"
        "html" -> "text/html"
        "png" -> "image/png"
        "txt" -> "text/plain"
        else -> "application/octet-stream"
    }

    private companion object {
        /** Имя задания в очереди печати системы. */
        const val JOB = "Superkassa"
    }
}
