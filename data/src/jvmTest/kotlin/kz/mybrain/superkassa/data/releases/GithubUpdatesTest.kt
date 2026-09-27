package kz.mybrain.superkassa.data.releases

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kz.mybrain.superkassa.domain.update.model.Fetched
import kz.mybrain.superkassa.domain.update.model.Installer
import kz.mybrain.superkassa.domain.update.model.ReleaseAnswer
import kz.mybrain.superkassa.integrations.releases.GithubReleases
import kz.mybrain.superkassa.integrations.releases.ReleasePlatform
import kz.mybrain.superkassa.integrations.releases.ReleaseSource
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Выпуски GitHub глазами обновлений: установщик под систему и сверка скачанного.
 *
 * Скачанное кладётся во временный каталог: прогон не трогает рабочее место.
 */
class GithubUpdatesTest {

    private val folder = createTempDirectory("superkassa-installers").toFile()

    @AfterTest
    fun clean() {
        folder.deleteRecursively()
    }

    private fun github(status: HttpStatusCode, body: String) = GithubReleases(
        ReleaseSource(),
        MockEngine { respond(body, status, headersOf(HttpHeaders.ContentType, "application/json")) }
    )

    private fun download(bytes: ByteArray, status: HttpStatusCode = HttpStatusCode.OK) =
        InstallerDownload(folder, HttpClient(MockEngine { respond(bytes, status) }))

    @Test
    fun `из выпуска берётся установщик этой системы с его суммой`() {
        val updates = GithubUpdates(github(HttpStatusCode.OK, LATEST), download(ByteArray(0)), ReleasePlatform.Windows)
        val release = assertIs<ReleaseAnswer.Found>(runBlocking { updates.latest() }).release
        assertEquals("v1.0.3", release.tag)
        assertEquals(Installer("Superkassa-1.0.3.msi", MSI_URL, SHA_OF_BODY), release.installer)
    }

    @Test
    fun `неизвестная система — установщика нет, открывают страницу`() {
        val updates = GithubUpdates(github(HttpStatusCode.OK, LATEST), download(ByteArray(0)), platform = null)
        assertEquals(null, assertIs<ReleaseAnswer.Found>(runBlocking { updates.latest() }).release.installer)
    }

    @Test
    fun `Android берёт из того же выпуска свой APK`() {
        val answer = runBlocking { github(HttpStatusCode.OK, LATEST).latestFor(ReleasePlatform.Android) }
        val installer = assertIs<ReleaseAnswer.Found>(answer).release.installer
        assertEquals("Superkassa-1.0.3.apk", installer?.name)
    }

    @Test
    fun `APK ещё не доложен в выпуск — Android ждёт его`() {
        val android = runBlocking { github(HttpStatusCode.OK, DESKTOP_ONLY).latestFor(ReleasePlatform.Android) }
        assertTrue(assertIs<ReleaseAnswer.Found>(android).release.installerPending)
        val windows = runBlocking { github(HttpStatusCode.OK, DESKTOP_ONLY).latestFor(ReleasePlatform.Windows) }
        assertFalse(assertIs<ReleaseAnswer.Found>(windows).release.installerPending)
    }

    @Test
    fun `отказ GitHub — недоступность, а не падение`() {
        val updates = GithubUpdates(github(HttpStatusCode.Forbidden, "{}"), download(ByteArray(0)))
        assertIs<ReleaseAnswer.Unreachable>(runBlocking { updates.latest() })
    }

    @Test
    fun `совпавший установщик лежит на диске и назван`() {
        val fetched = runBlocking { download(BODY).fetch(Installer("Superkassa-1.0.3.msi", MSI_URL, SHA_OF_BODY)) }
        val file = File(assertIs<Fetched.Verified>(fetched).file)
        assertTrue(file.readBytes().contentEquals(BODY))
    }

    @Test
    fun `подменённый установщик удаляется`() {
        val tampered = "not the installer".toByteArray()
        val fetched = runBlocking { download(tampered).fetch(Installer("Superkassa-1.0.3.msi", MSI_URL, SHA_OF_BODY)) }
        assertEquals(Fetched.Mismatch, fetched)
        assertFalse(File(folder, "Superkassa-1.0.3.msi").exists(), "подменённый файл остался на диске")
    }

    @Test
    fun `имя из сети не выводит файл за пределы каталога`() {
        val fetched = runBlocking { download(BODY).fetch(Installer("../../evil.msi", MSI_URL, SHA_OF_BODY)) }
        assertEquals(folder, File(assertIs<Fetched.Verified>(fetched).file).parentFile)
    }

    @Test
    fun `отказ сервера и обрыв — неудача, неполного файла нет`() {
        val refused = runBlocking {
            download(BODY, HttpStatusCode.NotFound).fetch(Installer("a.msi", MSI_URL, SHA_OF_BODY))
        }
        assertIs<Fetched.Failed>(refused)
        val broken = InstallerDownload(folder, HttpClient(MockEngine { throw IOException("reset") }))
        assertIs<Fetched.Failed>(runBlocking { broken.fetch(Installer("b.msi", MSI_URL, SHA_OF_BODY)) })
        assertTrue(folder.listFiles().orEmpty().isEmpty(), "остался неполный файл")
    }

    private companion object {
        const val MSI_URL = "https://example.test/releases/download/v1.0.3/Superkassa-1.0.3.msi"

        val BODY = "installer bytes".toByteArray()

        val SHA_OF_BODY: String = MessageDigest.getInstance("SHA-256").digest(BODY).toHexString()

        val DESKTOP_ONLY = """
            {
              "html_url": "https://github.com/texport/superkassa-desktop/releases/tag/v1.0.3",
              "tag_name": "v1.0.3",
              "name": "Суперкасса 1.0.3",
              "assets": [
                {"name": "Superkassa-1.0.3.dmg", "size": 1, "digest": "sha256:00",
                 "browser_download_url": "https://example.test/Superkassa-1.0.3.dmg"},
                {"name": "Superkassa-1.0.3.msi", "size": 1, "digest": "sha256:$SHA_OF_BODY",
                 "browser_download_url": "$MSI_URL"}
              ]
            }
        """.trimIndent()

        /** Тот же выпуск, когда в него уже доложен APK. */
        val LATEST = DESKTOP_ONLY.replace(
            "\"$MSI_URL\"}",
            "\"$MSI_URL\"}, {\"name\": \"Superkassa-1.0.3.apk\", \"size\": 1, \"digest\": \"sha256:$SHA_OF_BODY\", " +
                "\"browser_download_url\": \"https://example.test/Superkassa-1.0.3.apk\"}"
        )
    }
}
