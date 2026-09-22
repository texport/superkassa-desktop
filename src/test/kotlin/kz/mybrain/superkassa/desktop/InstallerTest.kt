package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.app.Installer
import kz.mybrain.superkassa.desktop.app.installerFor
import kz.mybrain.superkassa.desktop.server.releases.Release
import kz.mybrain.superkassa.desktop.server.releases.ReleaseAsset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Какой файл выпуска предлагается на какой системе. */
class InstallerTest {

    private val release = Release(
        tag = "v1.0.3",
        page = "https://github.com/texport/superkassa-desktop/releases/tag/v1.0.3",
        assets = listOf(
            ReleaseAsset("Superkassa-1.0.3.dmg", "https://example.test/Superkassa-1.0.3.dmg"),
            ReleaseAsset("Superkassa-1.0.3.msi", "https://example.test/Superkassa-1.0.3.msi"),
            ReleaseAsset("superkassa_1.0.3_amd64.deb", "https://example.test/superkassa_1.0.3_amd64.deb")
        )
    )

    @Test
    fun `система опознаётся по имени в Java`() {
        assertEquals(Installer.Mac, Installer.forSystem("Mac OS X"))
        assertEquals(Installer.Windows, Installer.forSystem("Windows 11"))
        assertEquals(Installer.Debian, Installer.forSystem("Linux"))
        assertNull(Installer.forSystem("FreeBSD"))
        assertNull(Installer.forSystem(null))
    }

    @Test
    fun `каждой системе — свой установщик`() {
        assertEquals("Superkassa-1.0.3.dmg", release.installerFor(Installer.Mac)?.name)
        assertEquals("Superkassa-1.0.3.msi", release.installerFor(Installer.Windows)?.name)
        assertEquals("superkassa_1.0.3_amd64.deb", release.installerFor(Installer.Debian)?.name)
    }

    @Test
    fun `без подходящего файла установщика нет`() {
        assertNull(release.installerFor(null))
        assertNull(release.copy(assets = emptyList()).installerFor(Installer.Mac))
    }
}
