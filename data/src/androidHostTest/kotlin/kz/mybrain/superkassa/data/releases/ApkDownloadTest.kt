package kz.mybrain.superkassa.data.releases

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kz.mybrain.superkassa.domain.update.model.Fetched
import kz.mybrain.superkassa.domain.update.model.Installer
import java.io.File
import java.security.MessageDigest
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * APK новой версии на Android: установщику системы отдаётся только сверенный с выпуском.
 *
 * APK ставится поверх кассы со всеми её данными: подменённый по дороге файл
 * получил бы кассу целиком. Скачанное кладётся во временный каталог.
 */
class ApkDownloadTest {

    private val folder = createTempDirectory("superkassa-apk").toFile()

    @AfterTest
    fun clean() {
        folder.deleteRecursively()
    }

    private fun download(bytes: ByteArray, status: HttpStatusCode = HttpStatusCode.OK) =
        ApkDownload(folder, HttpClient(MockEngine { respond(bytes, status) }))

    @Test
    fun `совпавший APK лежит в кэше и назван`() {
        val fetched = runBlocking { download(BODY).fetch(APK) }
        assertTrue(File(assertIs<Fetched.Verified>(fetched).file).readBytes().contentEquals(BODY))
    }

    @Test
    fun `подменённый APK удаляется и не открывается`() {
        val fetched = runBlocking { download("not the apk".toByteArray()).fetch(APK) }
        assertEquals(Fetched.Mismatch, fetched)
        assertFalse(File(folder, APK.name).exists(), "подменённый APK остался в кэше")
    }

    @Test
    fun `отказ сервера — неудача, неполного файла нет`() {
        assertIs<Fetched.Failed>(runBlocking { download(BODY, HttpStatusCode.NotFound).fetch(APK) })
        assertTrue(folder.listFiles().orEmpty().isEmpty(), "остался неполный APK")
    }

    private companion object {
        val BODY = "apk bytes".toByteArray()
        val APK = Installer(
            name = "Superkassa-1.0.3.apk",
            url = "https://example.test/releases/download/v1.0.3/Superkassa-1.0.3.apk",
            sha256 = MessageDigest.getInstance("SHA-256").digest(BODY).toHexString()
        )
    }
}
