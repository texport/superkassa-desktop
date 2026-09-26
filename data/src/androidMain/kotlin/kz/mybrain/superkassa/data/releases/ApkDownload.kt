package kz.mybrain.superkassa.data.releases

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.isSuccess
import io.ktor.utils.io.jvm.javaio.copyTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kz.mybrain.superkassa.domain.update.model.Fetched
import kz.mybrain.superkassa.domain.update.model.Installer
import kz.mybrain.superkassa.integrations.releases.ReleaseFile
import java.io.File
import java.io.IOException
import java.security.DigestOutputStream
import java.security.MessageDigest

/**
 * Скачивание APK новой версии со сверкой контрольной суммы выпуска.
 *
 * То же правило, что у установщиков компьютера: сумма считается по ходу
 * записи и сверяется правилом модуля выпусков, не совпавший или недокачанный
 * файл удаляется сразу. Своё на Android — место (кэш приложения, откуда
 * файл отдаётся установщику системы) и движок OkHttp.
 *
 * @param folder куда класть APK: каталог в кэше приложения.
 */
class ApkDownload(private val folder: File, private val http: HttpClient = defaultHttpClient()) {

    /** Скачивает [installer] и сверяет его с суммой выпуска. */
    suspend fun fetch(installer: Installer): Fetched = withContext(Dispatchers.IO) {
        // Имя приходит из сети: берётся только само имя, без каталогов.
        val target = File(folder, File(installer.name).name)
        try {
            clear(target)
            val sum = download(installer.url, target)
            val declared = ReleaseFile(installer.name, installer.url, target.length(), installer.sha256)
            if (declared.matches(sum)) Fetched.Verified(target.path) else Fetched.Mismatch.also { target.delete() }
        } catch (failure: IOException) {
            target.delete()
            Fetched.Failed(failure::class.simpleName.orEmpty())
        }
    }

    /** Прежние APK удаляются: нужен только тот, что скачивается сейчас. */
    private fun clear(target: File) {
        folder.mkdirs()
        folder.listFiles().orEmpty().filter { it.isFile && it != target }.forEach { it.delete() }
    }

    /** @return сумма SHA-256 записанного шестнадцатеричной строкой. */
    private suspend fun download(url: String, target: File): String {
        val digest = MessageDigest.getInstance(SHA256)
        http.prepareGet(url).execute { response ->
            if (!response.status.isSuccess()) throw IOException("HTTP ${response.status.value}")
            DigestOutputStream(target.outputStream(), digest).use { response.bodyAsChannel().copyTo(it) }
        }
        return digest.digest().toHexString()
    }

    private companion object {
        const val SHA256 = "SHA-256"

        /** Соединение ждётся недолго, а сам файл — сколько идёт: в нём десятки мегабайт. */
        const val CONNECT_MS = 15_000L
        const val SILENCE_MS = 60_000L

        fun defaultHttpClient(): HttpClient = HttpClient(OkHttp) {
            expectSuccess = false
            install(HttpTimeout) {
                connectTimeoutMillis = CONNECT_MS
                socketTimeoutMillis = SILENCE_MS
            }
        }
    }
}
