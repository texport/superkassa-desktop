package kz.mybrain.superkassa.domain.update.usecase

import kotlinx.coroutines.runBlocking
import kz.mybrain.superkassa.domain.update.model.AvailableUpdate
import kz.mybrain.superkassa.domain.update.model.Fetched
import kz.mybrain.superkassa.domain.update.model.InstallOutcome
import kz.mybrain.superkassa.domain.update.model.Installer
import kz.mybrain.superkassa.domain.update.port.FakeReleases
import kz.mybrain.superkassa.domain.version.model.AppVersion
import kz.mybrain.superkassa.kassa.SilentJournal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Установка найденной версии: открывается только сверенный с выпуском установщик.
 *
 * Установщик ставится с правами владельца машины, и подменённый по дороге
 * файл получил бы кассу целиком — поэтому без суммы, с которой его сверить,
 * касса его не скачивает, а открывает страницу выпуска.
 */
class InstallUpdateTest {

    private val releases = FakeReleases()

    private val install = InstallUpdate(releases, SilentJournal)

    private fun update(installer: Installer?) = AvailableUpdate(AppVersion(1, 0, 3), PAGE, installer)

    @Test
    fun `сверенный установщик открывается`() {
        val outcome = runBlocking { install(update(MSI)) }
        assertEquals(InstallOutcome.Started, outcome)
        assertEquals(listOf("/downloads/Superkassa-1.0.3.msi"), releases.opened)
    }

    @Test
    fun `не совпавший установщик не открывается`() {
        releases.fetched = Fetched.Mismatch
        assertEquals(InstallOutcome.Tampered, runBlocking { install(update(MSI)) })
        assertTrue(releases.opened.isEmpty())
    }

    @Test
    fun `без объявленной суммы установщик не скачивается — открыта страница выпуска`() {
        val outcome = runBlocking { install(update(MSI.copy(sha256 = null))) }
        assertEquals(InstallOutcome.PageOpened, outcome)
        assertTrue(releases.downloaded.isEmpty(), "скачан установщик, который не с чем сверить")
        assertEquals(listOf(PAGE), releases.opened)
    }

    @Test
    fun `без своего установщика открыта страница выпуска`() {
        assertEquals(InstallOutcome.PageOpened, runBlocking { install(update(null)) })
        assertEquals(listOf(PAGE), releases.opened)
    }

    @Test
    fun `обрыв скачивания — неудача, ничего не открыто`() {
        releases.fetched = Fetched.Failed("IOException")
        assertEquals(InstallOutcome.Failed, runBlocking { install(update(MSI)) })
        assertTrue(releases.opened.isEmpty())
    }

    @Test
    fun `без разрешения системы установщик не скачивается — открыта настройка разрешения`() {
        releases.allowed = false
        assertEquals(InstallOutcome.NeedsPermission, runBlocking { install(update(APK)) })
        assertTrue(releases.downloaded.isEmpty(), "установщик скачан, хотя система его не откроет")
        assertEquals(listOf(FakeReleases.INSTALL_PERMISSION), releases.opened)
    }

    private companion object {
        const val PAGE = "https://github.com/texport/superkassa-desktop/releases/tag/v1.0.3"
        val APK = Installer("Superkassa-1.0.3.apk", "https://example.test/Superkassa-1.0.3.apk", "cd34")
        val MSI = Installer("Superkassa-1.0.3.msi", "https://example.test/Superkassa-1.0.3.msi", "ab12")
    }
}
