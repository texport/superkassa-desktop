package kz.mybrain.superkassa.data.local

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kz.mybrain.superkassa.domain.cabinet.port.SavedFiles
import java.io.IOException

/**
 * Сохранение файла на Android — системным окном «Сохранить»: папку и имя
 * выбирает владелец, а касса пишет туда, куда ей разрешили.
 *
 * Заголовка у окна системы нет: окно подписывается видом файла.
 *
 * @param screen активность на экране: окно открывается поверх неё.
 */
class DocumentFiles(private val screen: ForegroundActivity) : SavedFiles {

    override suspend fun save(bytes: ByteArray, name: String, title: String): String? {
        val target = screen.createDocument(name, mimeOf(name)) ?: return null
        withContext(Dispatchers.IO) {
            val out = screen.activity?.contentResolver?.openOutputStream(target) ?: throw IOException(NO_SCREEN)
            out.use { it.write(bytes) }
        }
        return name
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
        const val NO_SCREEN = "no screen to write the document from"
    }
}
