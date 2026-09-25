package kz.mybrain.superkassa.presentation.update.check

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.io.files.Path
import kz.mybrain.superkassa.data.local.UpdatePreferences
import kz.mybrain.superkassa.domain.update.model.Fetched
import kz.mybrain.superkassa.domain.update.model.Installer
import kz.mybrain.superkassa.domain.update.model.Release
import kz.mybrain.superkassa.domain.update.model.ReleaseAnswer
import kz.mybrain.superkassa.domain.update.model.UpdateOutcome
import kz.mybrain.superkassa.domain.update.port.FakeReleases
import kz.mybrain.superkassa.domain.update.port.UpdateMemory
import kz.mybrain.superkassa.domain.update.port.UpdatePorts
import kz.mybrain.superkassa.domain.version.model.AppVersion
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.FakeCore
import kz.mybrain.superkassa.kassa.services
import kz.mybrain.superkassa.presentation.common.message.Message
import kz.mybrain.superkassa.presentation.common.message.Notices
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * Проверка выпусков: что считается новой версией и что остаётся после неё.
 *
 * Память о проверке пишется во временный каталог, а не в `~/.superkassa`:
 * прогон проверок не должен трогать настоящее рабочее место.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UpdatesViewModelTest {

    private val directory = createTempDirectory("superkassa-updates").toFile()

    private val moment = Instant.parse("2026-09-22T08:00:00Z")

    private val releases = FakeReleases(ReleaseAnswer.Found(LATEST))

    private val notices = Notices()

    private val texts = textsOf(Language.Ru).update

    @BeforeTest
    fun main() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun reset() {
        Dispatchers.resetMain()
        directory.deleteRecursively()
    }

    private fun model(installed: String, memory: UpdateMemory = UpdatePreferences(Path(directory.path))) = updatesModel(
        services = CoreScene.services(FakeCore(), notices = notices),
        ports = UpdatePorts(releases, memory),
        installed = AppVersion.parse(installed)!!,
        now = { moment }
    )

    @Test
    fun `новый выпуск найден и назван вместе с установщиком под систему`() {
        val model = model("1.0.2")
        model.check()
        val state = model.state.value
        val found = assertIs<UpdateOutcome.Available>(state.outcome).update
        assertEquals(AppVersion(1, 0, 3), found.version)
        assertEquals("Superkassa-1.0.3.msi", found.installer?.name)
        assertEquals(found, state.available)
        assertEquals(moment, state.lastChecked)
        assertEquals(moment, UpdatePreferences(Path(directory.path)).lastChecked)
    }

    @Test
    fun `установленная версия — последняя`() {
        val model = model("1.0.3")
        model.check()
        assertEquals(UpdateOutcome.UpToDate, model.state.value.outcome)
        assertNull(model.state.value.available)
    }

    @Test
    fun `сборка разработчика с теми же числами считается устаревшей`() {
        val model = model("1.0.3-dev")
        model.check()
        assertIs<UpdateOutcome.Available>(model.state.value.outcome)
    }

    @Test
    fun `нет связи — недоступность, время проверки не записывается`() {
        releases.answer = ReleaseAnswer.Unreachable("Network is unreachable")
        val model = model("1.0.2")
        model.check()
        val state = model.state.value
        assertEquals(UpdateOutcome.Unreachable, state.outcome)
        assertNull(state.available)
        assertNull(state.lastChecked)
        assertNull(UpdatePreferences(Path(directory.path)).lastChecked)
        assertFalse(state.checking)
    }

    @Test
    fun `выключенная проверка помнится рабочим местом`() {
        val model = model("1.0.2")
        assertTrue(model.state.value.automatic, "по умолчанию проверка выключена")
        model.switchAutomatic(false)
        assertFalse(UpdatePreferences(Path(directory.path)).automatic)
        assertFalse(model.state.value.automatic)
        model.switchAutomatic(true)
        assertTrue(UpdatePreferences(Path(directory.path)).automatic)
    }

    /** «Скачать» скачивает установщик, сверяет его и открывает — ставит кассир. */
    @Test
    fun `сверенный установщик открывается, кассиру сказано`() {
        val model = model("1.0.2")
        model.check()
        model.install(model.state.value.available!!)
        assertEquals(listOf("Superkassa-1.0.3.msi"), releases.downloaded.map { it.name })
        assertEquals(listOf("/downloads/Superkassa-1.0.3.msi"), releases.opened)
        assertEquals(Message.Done(texts.installerOpened), notices.last)
        assertFalse(model.state.value.installing)
    }

    /** Подменённый по дороге установщик не открывается: он получил бы машину целиком. */
    @Test
    fun `не совпавший установщик не открывается, кассиру сказан отказ`() {
        releases.fetched = Fetched.Mismatch
        val model = model("1.0.2")
        model.check()
        model.install(model.state.value.available!!)
        assertTrue(releases.opened.isEmpty(), "открыт подменённый установщик: ${releases.opened}")
        assertEquals(Message.Refusal(texts.installerTampered, "INSTALLER_CHECKSUM"), notices.last)
    }

    /** Проверка по расписанию ждёт своего часа: при открытии окна службу не спрашивают. */
    @Test
    fun `открытие окна службу выпусков не спрашивает`() {
        model("1.0.2")
        assertEquals(0, releases.asked)
    }

    private companion object {
        val LATEST = Release(
            tag = "v1.0.3",
            page = "https://github.com/texport/superkassa-desktop/releases/tag/v1.0.3",
            installer = Installer("Superkassa-1.0.3.msi", "https://example.test/Superkassa-1.0.3.msi", "ab12")
        )
    }
}
