package kz.mybrain.superkassa.integrations.releases

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * Разбор ответа GitHub о последнем выпуске.
 *
 * Тело снято с настоящего ответа `releases/latest`: из него нужны метка,
 * страница, файлы и их суммы, остальное не читается и ломать разбор не должно.
 */
class GithubReleasesTest {

    private var asked = ""

    private fun releases(status: HttpStatusCode, body: String) = GithubReleases(
        engine = MockEngine { request ->
            asked = request.url.toString()
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
    )

    @Test
    fun latestReleaseGivesTagPageAndFiles() = runTest {
        val release = assertIs<ReleaseAnswer.Found>(releases(HttpStatusCode.OK, LATEST).latest()).release

        assertEquals("https://api.github.com/repos/texport/superkassa-desktop/releases/latest", asked)
        assertEquals("v1.0.3", release.tag)
        assertEquals("https://github.com/texport/superkassa-desktop/releases/tag/v1.0.3", release.page)
        assertEquals("Суперкасса 1.0.3", release.title)
        val names = listOf(
            "Superkassa-1.0.3.dmg",
            "Superkassa-1.0.3.msi",
            "superkassa_1.0.3_amd64.deb",
            "Superkassa-1.0.3.apk"
        )
        assertEquals(names, release.files.map { it.name })
        assertEquals(84_213_760L, release.files[1].size)
    }

    @Test
    fun checksumIsReadFromDigest() = runTest {
        val files = assertIs<ReleaseAnswer.Found>(releases(HttpStatusCode.OK, LATEST).latest()).release.files

        assertEquals("5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8", files[0].sha256)
        assertNull(files[2].sha256, "у файла без digest суммы нет")
        assertEquals(true, files[0].matches("5E884898DA28047151D0E56F8DC6292773603D0D6AABBDD62A11EF721D1542D8"))
        assertEquals(false, files[2].matches("5e884898"))
    }

    @Test
    fun fileIsChosenForTheSystem() = runTest {
        val release = assertIs<ReleaseAnswer.Found>(releases(HttpStatusCode.OK, LATEST).latest()).release

        assertEquals("Superkassa-1.0.3.dmg", release.fileFor(ReleasePlatform.forSystem("Mac OS X"))?.name)
        assertEquals("Superkassa-1.0.3.msi", release.fileFor(ReleasePlatform.forSystem("Windows 11"))?.name)
        assertEquals("superkassa_1.0.3_amd64.deb", release.fileFor(ReleasePlatform.forSystem("Linux"))?.name)
        assertEquals("Superkassa-1.0.3.apk", release.fileFor(ReleasePlatform.Android)?.name)
        assertNull(release.fileFor(ReleasePlatform.forSystem("FreeBSD")))
        assertNull(release.fileFor(null))
    }

    @Test
    fun answerNotAboutReleaseIsUnreachable() = runTest {
        assertIs<ReleaseAnswer.Unreachable>(releases(HttpStatusCode.NotFound, "{}").latest())
        assertIs<ReleaseAnswer.Unreachable>(releases(HttpStatusCode.OK, "not json").latest())
        assertIs<ReleaseAnswer.Unreachable>(releases(HttpStatusCode.OK, "{}").latest())
        val limited = releases(HttpStatusCode.Forbidden, """{"message":"API rate limit exceeded"}""").latest()
        assertEquals(ReleaseAnswer.Unreachable("HTTP 403"), limited)
    }

    @Test
    fun silentNetworkIsUnreachable() = runTest {
        val silent = GithubReleases(engine = MockEngine { throw IOException("Network is unreachable") })

        assertEquals(ReleaseAnswer.Unreachable("Network is unreachable"), silent.latest())
    }

    private companion object {
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
                {"name": "Superkassa-1.0.3.dmg", "size": 91234567,
                 "digest": "sha256:5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8",
                 "browser_download_url": "https://github.com/texport/superkassa-desktop/releases/download/v1.0.3/Superkassa-1.0.3.dmg"},
                {"name": "Superkassa-1.0.3.msi", "size": 84213760,
                 "digest": "sha256:9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08",
                 "browser_download_url": "https://github.com/texport/superkassa-desktop/releases/download/v1.0.3/Superkassa-1.0.3.msi"},
                {"name": "superkassa_1.0.3_amd64.deb", "size": 1,
                 "browser_download_url": "https://github.com/texport/superkassa-desktop/releases/download/v1.0.3/superkassa_1.0.3_amd64.deb"},
                {"name": "Superkassa-1.0.3.apk", "size": 40000000,
                 "digest": "sha256:2c26b46b68ffc68ff99b453c1d30413413422d706483bfa0f98a5e886266e7ae",
                 "browser_download_url": "https://github.com/texport/superkassa-desktop/releases/download/v1.0.3/Superkassa-1.0.3.apk"}
              ],
              "body": "Что нового"
            }
        """.trimIndent()
    }
}
