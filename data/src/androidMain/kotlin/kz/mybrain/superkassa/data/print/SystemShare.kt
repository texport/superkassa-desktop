package kz.mybrain.superkassa.data.print

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kz.mybrain.superkassa.data.local.ForegroundActivity
import kz.mybrain.superkassa.domain.print.model.ShareWay
import kz.mybrain.superkassa.domain.print.model.SharedReceipt
import kz.mybrain.superkassa.domain.print.port.ShareOut
import java.io.File
import java.io.IOException

/**
 * Поделиться чеком на Android — системным окном «Поделиться».
 *
 * Android сам предлагает то, чем покупателю пишут и так: WhatsApp, Telegram,
 * почту, SMS. Касса отдаёт файл формы из своего кэша ссылкой `content://`
 * через поставщик файлов и слова со ссылкой на электронный чек; кому и что
 * отправить, выбирает кассир в окне системы.
 *
 * @param screen активность на экране: из неё открывается окно системы.
 */
class SystemShare(private val context: Context, private val screen: ForegroundActivity) : ShareOut {

    override val ways: List<ShareWay> = listOf(ShareWay.System)

    override suspend fun share(receipt: SharedReceipt, way: ShareWay): Boolean {
        val file = withContext(Dispatchers.IO) { stored(receipt) } ?: return false
        val uri = FileProvider.getUriForFile(context, "${context.packageName}$AUTHORITY", file)
        val send = Intent(Intent.ACTION_SEND)
            .setType(receipt.mime)
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_SUBJECT, receipt.subject)
            .putExtra(Intent.EXTRA_TEXT, receipt.text)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        // Разрешение на файл уходит выбранной программе через ClipData:
        // окно выбора само его дальше не передаёт.
        send.clipData = ClipData.newRawUri(receipt.name, uri)
        return withContext(Dispatchers.Main) { start(Intent.createChooser(send, receipt.subject)) }
    }

    /** Файл формы в кэше; прежние удаляются — нужен только тот, что отдают сейчас. */
    private fun stored(receipt: SharedReceipt): File? = try {
        val folder = File(context.cacheDir, FOLDER).apply { mkdirs() }
        folder.listFiles().orEmpty().forEach { it.delete() }
        // Имя складывает касса, но берётся только само имя, без каталогов.
        File(folder, File(receipt.name).name).apply { writeBytes(receipt.bytes) }
    } catch (_: IOException) {
        null
    }

    private fun start(intent: Intent): Boolean = try {
        val shown = screen.activity
        if (shown != null) shown.startActivity(intent) else context.startActivity(intent.addFlags(NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }

    private companion object {
        /** Каталог отданных форм в кэше приложения; его же отдаёт поставщик файлов. */
        const val FOLDER = "shared"

        /** Окончание имени поставщика файлов после имени пакета — как в манифесте. */
        const val AUTHORITY = ".files"

        const val NEW_TASK = Intent.FLAG_ACTIVITY_NEW_TASK
    }
}
