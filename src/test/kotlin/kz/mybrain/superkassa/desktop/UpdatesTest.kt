package kz.mybrain.superkassa.desktop

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kz.mybrain.superkassa.desktop.app.AppVersion
import kz.mybrain.superkassa.desktop.app.Installer
import kz.mybrain.superkassa.desktop.app.UpdateOutcome
import kz.mybrain.superkassa.desktop.app.UpdatePreferences
import kz.mybrain.superkassa.desktop.app.Updates
import kz.mybrain.superkassa.desktop.server.releases.ReleaseClient
import java.io.IOException
import java.time.Instant
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Проверка выпусков: что считается новой версией и что остаётся после неё.
 *
 * Настройки пишутся во временный каталог, а не в `~/.superkassa`:
 * прогон проверок не должен трогать настоящее рабочее место.
 */
class UpdatesTest {

    private val directory = createTempDirectory("superkassa-updates").toFile()

    private val moment = Instant.parse("2026-09-22T08:00:00Z")

    private fun updates(installed: String, body: String, status: HttpStatusCode = HttpStatusCode.OK): Updates {
        val engine = MockEngine {
            if (body.isEmpty()) throw IOException("no route")
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        return Updates(
            preferences = UpdatePreferences(directory),
            client = ReleaseClient(http = HttpClient(engine) { expectSuccess = false }),
            installed = AppVersion.parse(installed)!!,
            installer = Installer.Windows,
            now = { moment }
        )
    }

    @Test
    fun `новый выпуск найден и назван вместе с установщиком под систему`() {
        val updates = updates("1.0.2", ReleaseClientTest.LATEST)
        val outcome = runBlocking { updates.check() }
        val found = assertIs<UpdateOutcome.Available>(outcome).update
        assertEquals(AppVersion(1, 0, 3), found.version)
        assertTrue(found.download!!.endsWith("Superkassa-1.0.3.msi"), "установщик не под Windows: ${found.download}")
        assertEquals(found, updates.available)
        assertEquals(moment, updates.lastChecked)
        assertEquals(moment, UpdatePreferences(directory).lastChecked)
    }

    @Test
    fun `установленная версия — последняя`() {
        val updates = updates("1.0.3", ReleaseClientTest.LATEST)
        assertEquals(UpdateOutcome.UpToDate, runBlocking { updates.check() })
        assertNull(updates.available)
    }

    @Test
    fun `сборка разработчика с теми же числами считается устаревшей`() {
        val updates = updates("1.0.3-dev", ReleaseClientTest.LATEST)
        assertIs<UpdateOutcome.Available>(runBlocking { updates.check() })
    }

    @Test
    fun `нет связи — недоступность, время проверки не записывается`() {
        val updates = updates("1.0.2", body = "")
        assertEquals(UpdateOutcome.Unreachable, runBlocking { updates.check() })
        assertNull(updates.available)
        assertNull(updates.lastChecked)
        assertFalse(updates.checking)
    }

    @Test
    fun `выключенная проверка помнится рабочим местом`() {
        val updates = updates("1.0.2", ReleaseClientTest.LATEST)
        assertTrue(updates.automatic, "по умолчанию проверка выключена")
        updates.switchAutomatic(false)
        assertFalse(UpdatePreferences(directory).automatic)
        updates.switchAutomatic(true)
        assertTrue(UpdatePreferences(directory).automatic)
    }
}
