package kz.mybrain.superkassa.presentation.settings.core

import io.github.texport.superkassa.embedded.api.SuperkassaPlatform
import io.github.texport.superkassa.testing.api.kassa.TestBench
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kz.mybrain.superkassa.data.kassa.EmbeddedKassa
import kz.mybrain.superkassa.data.kassa.settings.EmbeddedSettings
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.FakeCore
import kz.mybrain.superkassa.kassa.app
import kz.mybrain.superkassa.presentation.common.message.Message
import kz.mybrain.superkassa.presentation.common.message.Notices
import kz.mybrain.superkassa.presentation.settings.settingsPorts
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Настройки кассы с экрана — в настоящую кассу на временном каталоге.
 *
 * Касса поднята с настройками запуска приложения, но на тестовом БФД
 * и часах оснастки ядра: наружу проверка не ходит. Как хранит настройки
 * сама касса, проверяет `EmbeddedSettingsTest` слоя данных.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CoreSettingsOnKassaTest {
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

    private fun store() = EmbeddedSettings(bench.superkassa.settings, io = Dispatchers.Unconfined)

    @Test
    fun `сохранённое ожидание читается обратно`() {
        val notices = Notices()
        val model = coreSettingsModel(
            CoreScene.app(FakeCore(), notices = notices, settings = settingsPorts().copy(coreSettings = store()))
        )

        model.typeTimeout("9")
        model.save()

        assertEquals(Message.Done(textsOf(Language.Ru).settings.core.saved), notices.last)
        assertEquals(9L, runBlocking { store().read() }.ofdTimeoutSeconds)
    }
}
