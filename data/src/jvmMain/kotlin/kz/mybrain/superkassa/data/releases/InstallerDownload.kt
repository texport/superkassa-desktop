package kz.mybrain.superkassa.data.releases

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
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
import java.nio.channels.UnresolvedAddressException
import java.security.DigestOutputStream
import java.security.MessageDigest

/**
 * Скачивание установщика со сверкой контрольной суммы.
 *
 * Сумма считается по ходу записи, а сверяется правилом модуля выпусков:
 * не совпавший файл удаляется сразу, чтобы его нельзя было открыть по
 * ошибке. Неполный файл после обрыва связи удаляется так же.
 *
 * @param folder куда класть установщики.
 */
class InstallerDownload(private val folder: File, private val http: HttpClient = defaultHttpClient()) {

    suspend fun fetch(installer: Installer): Fetched = withContext(Dispatchers.IO) {
        // Имя приходит из сети: берётся только само имя, без каталогов.
        val target = File(folder, File(installer.name).name)
        try {
            clear(target)
            val sum = download(installer.url, target)
            val declared = ReleaseFile(installer.name, installer.url, target.length(), installer.sha256)
            if (declared.matches(sum)) Fetched.Verified(target.path) else Fetched.Mismatch.also { target.delete() }
        } catch (failure: IOException) {
            failed(target, failure)
        } catch (failure: UnresolvedAddressException) {
            failed(target, failure)
        }
    }

    /**
     * Прежние установщики удаляются: каждый весит сотню мегабайт, а нужен
     * только тот, что скачивается сейчас.
     */
    private fun clear(target: File) {
        folder.mkdirs()
        folder.listFiles().orEmpty().filter { it.isFile && it != target }.forEach { it.delete() }
    }

    private fun failed(target: File, failure: Exception): Fetched {
        target.delete()
        return Fetched.Failed(failure::class.simpleName.orEmpty())
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

        /** Соединение ждётся недолго, а сам файл — сколько идёт: он в сотню мегабайт. */
        const val CONNECT_MS = 15_000L
        const val SILENCE_MS = 60_000L

        fun defaultHttpClient(): HttpClient = HttpClient(CIO) {
            expectSuccess = false
            install(HttpTimeout) {
                connectTimeoutMillis = CONNECT_MS
                socketTimeoutMillis = SILENCE_MS
            }
        }
    }
}
