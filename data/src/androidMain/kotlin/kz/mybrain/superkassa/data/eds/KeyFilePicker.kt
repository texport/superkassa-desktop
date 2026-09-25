package kz.mybrain.superkassa.data.eds

import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kz.mybrain.superkassa.data.local.ForegroundActivity

/**
 * Файл ключа, выбранный владельцем: имя — чтобы он видел, каким ключом
 * подписывает, и содержимое — зашифрованное паролем, которого здесь нет.
 */
internal class KeyFile(val name: String, val bytes: ByteArray)

/** Откуда берётся файл ключа. */
internal fun interface KeyFiles {
    /** Выбранный файл; `null` — владелец не выбрал или файл не прочитался. */
    suspend fun pick(): KeyFile?
}

/**
 * Выбор файла ключа системным окном выбора файлов.
 *
 * Окно предлагает файлы любого вида: `.p12` у разных хранилищ числится
 * то ключом, то просто данными, и отобранный по виду файл владелец не нашёл
 * бы. Что это ключ, проверяет подпись.
 */
internal class KeyFilePicker(private val screen: ForegroundActivity) : KeyFiles {

    /** Выбранный файл; `null` — окно закрыли, экрана нет или файл не прочитался. */
    override suspend fun pick(): KeyFile? {
        val uri = screen.openDocument(arrayOf(ANY)) ?: return null
        return withContext(Dispatchers.IO) { runCatching { read(uri) }.getOrNull() }
    }

    private fun read(uri: Uri): KeyFile? {
        val resolver = screen.activity?.contentResolver ?: return null
        val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { row ->
            if (row.moveToFirst()) row.getString(0) else null
        }
        return resolver.openInputStream(uri)?.use { it.readBytes() }
            ?.let { KeyFile(name ?: uri.lastPathSegment.orEmpty(), it) }
    }

    private companion object {
        const val ANY = "*/*"
    }
}
