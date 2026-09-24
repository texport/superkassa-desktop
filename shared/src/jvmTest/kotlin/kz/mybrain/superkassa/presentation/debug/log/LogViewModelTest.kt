package kz.mybrain.superkassa.presentation.debug.log

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kz.mybrain.superkassa.domain.debug.model.LogEntry
import kz.mybrain.superkassa.domain.debug.model.LogLevel
import kz.mybrain.superkassa.domain.debug.model.LogSource
import kz.mybrain.superkassa.domain.debug.port.LogBookState
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.FakeCore
import kz.mybrain.superkassa.presentation.settings.MemoryLogBook
import kz.mybrain.superkassa.presentation.settings.settingsPorts
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Журнал отладки без окна: отбор, порог записи и сохранение показанного.
 *
 * Книга журнала — в памяти: файл рабочей машины проверка не трогает.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LogViewModelTest {

    private val book = MemoryLogBook(
        LogBookState(
            entries = listOf(
                entry(LogLevel.Debug, "GET /kkm body"),
                entry(LogLevel.Info, "POST /kkm/kkm-1/receipt 200"),
                entry(LogLevel.Failure, "POST /kkm/kkm-1/receipt 409 SHIFT_EXPIRED"),
                entry(LogLevel.Warning, "cabinet slow", LogSource.Cabinet)
            ),
            file = "/tmp/superkassa-log/superkassa.log"
        )
    )

    @BeforeTest
    fun main() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun reset() = Dispatchers.resetMain()

    @Test
    fun `отбор оставляет уровень не ниже выбранного и совпавшее со строкой`() {
        val model = logModel(CoreScene.app(FakeCore(), settings = settingsPorts().copy(logBook = book)))

        assertEquals(4, model.state.value.shown.size, "окно открывается со всем журналом")
        model.filter(LogLevel.Warning)
        assertEquals(2, model.state.value.shown.size)
        model.search("receipt")
        assertEquals(listOf("POST /kkm/kkm-1/receipt 409 SHIFT_EXPIRED"), model.state.value.shown.map { it.text })
    }

    @Test
    fun `порог записи и режим отладки уходят в журнал рабочего места`() {
        val model = logModel(CoreScene.app(FakeCore(), settings = settingsPorts().copy(logBook = book)))

        model.chooseLevel(LogLevel.Debug)
        model.switchDebugMode(true)

        assertEquals(LogLevel.Debug, book.state.value.level)
        assertTrue(book.state.value.debugMode)
        assertEquals(LogLevel.Debug, model.state.value.book.level, "карточка не видит выбранного порога")
        assertTrue(model.state.value.book.debugMode, "переключатель не видит включённого режима")
    }

    /** В поддержку пересылают разбор одного отказа, а не всю смену. */
    @Test
    fun `сохраняется то, что видно после отбора`() {
        val model = logModel(CoreScene.app(FakeCore(), settings = settingsPorts().copy(logBook = book)))

        model.filter(LogLevel.Failure)
        model.save()

        assertEquals(listOf(LogLevel.Failure), book.saved?.map { it.level })
    }

    @Test
    fun `очищенный журнал пуст и в окне`() {
        val model = logModel(CoreScene.app(FakeCore(), settings = settingsPorts().copy(logBook = book)))

        model.clear()

        assertTrue(model.state.value.shown.isEmpty())
    }

    private fun entry(level: LogLevel, text: String, source: LogSource = LogSource.Machine) =
        LogEntry(time = "2026-09-23 12:00:00.000", level = level, source = source, text = text)
}
