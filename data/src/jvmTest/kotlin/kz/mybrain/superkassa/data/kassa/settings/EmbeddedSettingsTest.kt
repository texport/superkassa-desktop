package kz.mybrain.superkassa.data.kassa.settings

import io.github.texport.superkassa.core.domain.api.model.settings.CoreMode
import io.github.texport.superkassa.embedded.api.SuperkassaPlatform
import io.github.texport.superkassa.testing.api.kassa.TestBench
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kz.mybrain.superkassa.data.kassa.EmbeddedKassa
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.model.answering
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
class EmbeddedSettingsTest {
    private val directory: File = createTempDirectory("kassa-settings-").toFile()
    private lateinit var bench: TestBench

    @BeforeTest
    fun open() {
        bench = TestBench.open(SuperkassaPlatform(directory.path), config = EmbeddedKassa.config())
    }

    @AfterTest
    fun close() {
        bench.close()
        directory.deleteRecursively()
    }

    private fun store() = EmbeddedSettings(bench.superkassa.settings, io = Dispatchers.Unconfined)

    @Test
    fun `касса рабочего места открыта для правки и работает по протоколу запуска`(): Unit = runBlocking {
        val settings = store().read()

        assertEquals(CoreMode.DESKTOP, settings.mode)
        assertTrue(settings.allowChanges, "правка настроек закрыта на свежем рабочем месте")
        assertEquals("204", settings.ofdProtocolVersion)
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
