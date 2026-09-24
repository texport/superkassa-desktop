package kz.mybrain.superkassa.data.print

import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import java.io.FileOutputStream

/**
 * Документ PDF для системной печати Android.
 *
 * Форму рисует касса: документ уже готов, и адаптер только отдаёт его
 * системе — для предпросмотра в диалоге и для самой печати. Страниц
 * не считает: касса рисует ленту одним листом, а раскладку по бумаге
 * выбранного принтера ведёт система.
 *
 * @param onFinish диалог печати закрыт: задание ушло или его отменили.
 */
internal class PdfPrintAdapter(
    private val pdf: ByteArray,
    private val name: String,
    private val onFinish: () -> Unit
) : PrintDocumentAdapter() {

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal,
        callback: LayoutResultCallback,
        extras: Bundle?
    ) {
        if (cancellationSignal.isCanceled) return callback.onLayoutCancelled()
        val info = PrintDocumentInfo.Builder(name).setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).build()
        callback.onLayoutFinished(info, oldAttributes != newAttributes)
    }

    override fun onWrite(
        pages: Array<out PageRange>,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal,
        callback: WriteResultCallback
    ) {
        if (cancellationSignal.isCanceled) return callback.onWriteCancelled()
        runCatching { FileOutputStream(destination.fileDescriptor).use { it.write(pdf) } }
            .onSuccess { callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES)) }
            .onFailure { callback.onWriteFailed(it::class.simpleName) }
    }

    override fun onFinish() = onFinish.invoke()
}
