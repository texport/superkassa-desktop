package kz.mybrain.superkassa.domain.update.usecase

import kotlinx.coroutines.test.runTest
import kz.mybrain.superkassa.domain.log.port.Journal
import kz.mybrain.superkassa.domain.update.model.Fetched
import kz.mybrain.superkassa.domain.update.model.Installer
import kz.mybrain.superkassa.domain.update.model.Release
import kz.mybrain.superkassa.domain.update.model.ReleaseAnswer
import kz.mybrain.superkassa.domain.update.model.UpdateOutcome
import kz.mybrain.superkassa.domain.update.port.Releases
import kz.mybrain.superkassa.domain.update.port.UpdateMemory
import kz.mybrain.superkassa.domain.version.model.AppVersion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Instant

/**
 * Какой выпуск касса предлагает поставить.
 *
 * Выпуск выходит с установщиками для компьютеров, APK докладывается в него
 * следом. Пока APK нет, Android не должна звать на страницу без своего файла.
 */
class CheckForUpdateTest {

    private val apk = Installer("Superkassa-1.0.7.apk", "https://example.test/Superkassa-1.0.7.apk", "00")

    private fun check(release: Release, installed: String = "1.0.6") = CheckForUpdate(
        releases = OneRelease(release),
        memory = Remembered(),
        journal = Silent,
        installed = requireNotNull(AppVersion.parse(installed)),
        now = { Instant.fromEpochMilliseconds(0) }
    )

    @Test
    fun `выпуск со своим установщиком предлагается`() = runTest {
        val outcome = check(Release("v1.0.7", PAGE, apk))()
        assertEquals(apk, assertIs<UpdateOutcome.Available>(outcome).update.installer)
    }

    @Test
    fun `свой файл ещё не доложен — обновления пока нет`() = runTest {
        assertEquals(UpdateOutcome.UpToDate, check(Release("v1.0.7", PAGE, installerPending = true))())
    }

    @Test
    fun `система неизвестна — выпуск предлагается страницей`() = runTest {
        val outcome = check(Release("v1.0.7", PAGE))()
        assertEquals(null, assertIs<UpdateOutcome.Available>(outcome).update.installer)
    }

    @Test
    fun `выпуск не новее установленного — ничего не предлагается`() = runTest {
        assertEquals(UpdateOutcome.UpToDate, check(Release("v1.0.7", PAGE, apk), installed = "1.0.7")())
    }

    private class OneRelease(private val release: Release) : Releases {
        override suspend fun latest(): ReleaseAnswer = ReleaseAnswer.Found(release)
        override suspend fun fetch(installer: Installer): Fetched = error("не скачивается")
        override fun openFile(file: String) = false
        override fun openPage(url: String) = false
    }

    private class Remembered : UpdateMemory {
        override var automatic: Boolean = true
        override var lastChecked: Instant? = null
    }

    private object Silent : Journal {
        override fun info(text: String) = Unit
        override fun warn(text: String) = Unit
        override fun failure(text: String) = Unit
    }

    private companion object {
        const val PAGE = "https://github.com/texport/superkassa-desktop/releases/tag/v1.0.7"
    }
}
