package kz.mybrain.superkassa.desktop

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kz.mybrain.superkassa.desktop.server.releases.ReleaseAnswer
import kz.mybrain.superkassa.desktop.server.releases.ReleaseClient
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Разбор ответа GitHub о последнем выпуске.
 *
 * Тело ниже снято с настоящего ответа `releases/latest`: из него нужны
 * метка, страница и файлы, остальное не читается и ломать разбор не должно.
 */
class ReleaseClientTest {

    private fun clientReturning(status: HttpStatusCode, body: String): ReleaseClient {
        val engine = MockEngine {
            respond(
                content = body,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        return ReleaseClient(http = HttpClient(engine) { expectSuccess = false })
    }

    @Test
    fun `из ответа читаются метка, страница и файлы`() {
        val client = clientReturning(HttpStatusCode.OK, LATEST)
        val answer = runBlocking { client.latest() }
        val release = assertIs<ReleaseAnswer.Found>(answer).release
        assertEquals("v1.0.3", release.tag)
        assertEquals("https://github.com/texport/superkassa-desktop/releases/tag/v1.0.3", release.page)
        assertEquals(
            listOf("Superkassa-1.0.3.dmg", "Superkassa-1.0.3.msi", "superkassa_1.0.3_amd64.deb"),
            release.assets.map { it.name }
        )
        assertEquals(
            "https://github.com/texport/superkassa-desktop/releases/download/v1.0.3/Superkassa-1.0.3.msi",
            release.assets[1].url
        )
    }

    @Test
    fun `ответ не о выпуске — недоступность, а не падение`() {
        assertIs<ReleaseAnswer.Unreachable>(runBlocking { clientReturning(HttpStatusCode.NotFound, "{}").latest() })
        assertIs<ReleaseAnswer.Unreachable>(runBlocking { clientReturning(HttpStatusCode.OK, "not json").latest() })
        assertIs<ReleaseAnswer.Unreachable>(
            runBlocking { clientReturning(HttpStatusCode.Forbidden, """{"message":"rate limit"}""").latest() }
        )
    }

    @Test
    fun `сеть молчит — недоступность`() {
        val engine = MockEngine { throw IOException("Network is unreachable") }
        val client = ReleaseClient(http = HttpClient(engine) { expectSuccess = false })
        val answer = runBlocking { client.latest() }
        assertEquals("Network is unreachable", assertIs<ReleaseAnswer.Unreachable>(answer).reason)
    }

    companion object {
        val LATEST = """
            {
              "url": "https://api.github.com/repos/texport/superkassa-desktop/releases/1",
              "html_url": "https://github.com/texport/superkassa-desktop/releases/tag/v1.0.3",
              "id": 1,
              "tag_name": "v1.0.3",
              "name": "Суперкасса 1.0.3",
              "draft": false,
              "prerelease": false,
              "published_at": "2026-09-20T10:00:00Z",
              "assets": [
                {"name": "Superkassa-1.0.3.dmg", "size": 1,
                 "browser_download_url": "https://github.com/texport/superkassa-desktop/releases/download/v1.0.3/Superkassa-1.0.3.dmg"},
                {"name": "Superkassa-1.0.3.msi", "size": 1,
                 "browser_download_url": "https://github.com/texport/superkassa-desktop/releases/download/v1.0.3/Superkassa-1.0.3.msi"},
                {"name": "superkassa_1.0.3_amd64.deb", "size": 1,
                 "browser_download_url": "https://github.com/texport/superkassa-desktop/releases/download/v1.0.3/superkassa_1.0.3_amd64.deb"}
              ],
              "body": "Что нового"
            }
        """.trimIndent()
    }
}
