package kz.mybrain.superkassa.data.kassa.settings

import io.github.texport.superkassa.core.domain.api.model.settings.CoreMode
import io.github.texport.superkassa.embedded.api.SuperkassaPlatform
import io.github.texport.superkassa.testing.api.kassa.TestBench
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kz.mybrain.superkassa.data.kassa.EmbeddedKassa
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.model.answering
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.FakeCore
import kz.mybrain.superkassa.presentation.common.message.Message
import kz.mybrain.superkassa.presentation.common.message.Notices
import kz.mybrain.superkassa.presentation.settings.core.coreSettingsModel
import kz.mybrain.superkassa.presentation.settings.settingsPorts
import kz.mybrain.superkassa.presentation.strings.common.Language
import kz.mybrain.superkassa.presentation.strings.settings.coreSettingTexts
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Настройки настоящей кассы на временном каталоге.
 *
 * Касса поднята с настройками запуска приложения, но на тестовом БФД
 * и часах оснастки ядра: наружу проверка не ходит. Каталог рабочего места
 * не трогается: у каждой проверки свой временный.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EmbeddedSettingsTest {
    private val directory: File = createTempDirectory("kassa-settings-").toFile()
    private lateinit var bench: TestBench

    @BeforeTest
    fun open() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        bench = TestBench.open(SuperkassaPlatform(directory.path), config = EmbeddedKassa.config())
    }

    @AfterTest
    fun close() {
        bench.close()
        directory.deleteRecursively()
        Dispatchers.resetMain()
    }

    private fun store() = EmbeddedSettings(bench.superkassa.settings, Dispatchers.Unconfined)

    @Test
    fun `касса рабочего места открыта для правки и работает по протоколу запуска`(): Unit = runBlocking {
        val settings = store().read()

        assertEquals(CoreMode.DESKTOP, settings.mode)
        assertTrue(settings.allowChanges, "правка настроек закрыта на свежем рабочем месте")
        assertEquals("204", settings.ofdProtocolVersion)
    }

    @Test
    fun `сохранённое ожидание читается обратно`() {
        val notices = Notices()
        val model = coreSettingsModel(
            CoreScene.app(FakeCore(), notices = notices, settings = settingsPorts().copy(coreSettings = store()))
        )

        model.typeTimeout("9")
        model.save()

        assertEquals(Message.Done(coreSettingTexts(Language.Ru).saved), notices.last)
        assertEquals(9L, runBlocking { store().read() }.ofdTimeoutSeconds)
    }

    /** Версию протокола задаёт запуск: касса отказывает её сменой, а не молча не применяет. */
    @Test
    fun `версия протокола через настройки не меняется`(): Unit = runBlocking {
        val now = store().read()

        val answer = answering { store().save(now.copy(ofdProtocolVersion = "203")) }

        assertEquals("SETTINGS_FROZEN", assertIs<Answer.Refused>(answer).code)
        assertFalse(answer.ru.isBlank())
    }
}
