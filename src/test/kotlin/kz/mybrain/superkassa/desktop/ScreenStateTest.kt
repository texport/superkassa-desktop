package kz.mybrain.superkassa.desktop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.ui.cabinet.PlaceTree
import kz.mybrain.superkassa.desktop.ui.components.ScreenSlot
import kz.mybrain.superkassa.desktop.ui.components.ScreenState
import kz.mybrain.superkassa.desktop.ui.history.JournalEmpty
import kz.mybrain.superkassa.desktop.ui.history.JournalQuery
import kz.mybrain.superkassa.desktop.ui.history.JournalView
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.cabinetTexts
import kz.mybrain.superkassa.desktop.ui.strings.journalTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Durations
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Общее место содержимого: ожидание, пустота, отказ и само содержимое.
 *
 * Проверяется то, ради чего элемент заведён один на всё приложение:
 * содержимое показывается только в готовом состоянии, а во всех
 * остальных на его месте стоит объяснение, а не пустота.
 */
class ScreenStateTest {

    private val journal = journalTexts(Language.Ru).history
    private val cabinet = cabinetTexts(Language.Ru)

    private fun probe(state: ScreenState, onContent: () -> Unit = {}) = RenderProbe {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenSlot(state, Modifier.fillMaxSize()) {
                onContent()
                Text("содержимое")
            }
        }
    }

    @Test
    fun `готовое состояние показывает содержимое`() {
        var drawn = false
        probe(ScreenState.Ready) { drawn = true }.use { it.frame() }
        assertTrue(drawn, "в готовом состоянии рисуется то, ради чего экран открывали")
    }

    @Test
    fun `ожидание не пускает содержимое на экран`() {
        var drawn = false
        probe(ScreenState.Working) { drawn = true }.use { it.frame() }
        assertFalse(drawn, "пока ответа нет, показывать нечего")
    }

    @Test
    fun `пустота не пускает содержимое на экран`() {
        var drawn = false
        val empty = ScreenState.Empty(AppIcons.noDocuments, "Документов нет", "Пробейте чек")
        probe(empty).use { it.frame() }
        probe(empty) { drawn = true }.use { it.frame() }
        assertFalse(drawn)
    }

    @Test
    fun `отказ не пускает содержимое и повторяет только по нажатию`() {
        var drawn = false
        var retried = 0
        val trouble = ScreenState.Trouble("Служба не отвечает", "Проверьте связь") { retried += 1 }
        probe(trouble) { drawn = true }.use { it.frame() }
        assertFalse(drawn)
        assertEquals(0, retried, "повтор происходит по нажатию, а не сам собой")
    }

    /**
     * Мгновенный ответ не мигает.
     *
     * Кружок ждёт общей паузы, и первый кадр ожидания ничем не отличается
     * от пустой сцены: ответ, пришедший быстрее, снимает элемент раньше,
     * чем владелец что-то увидит.
     */
    @Test
    fun `кружок не успевает мелькнуть на первом кадре`() {
        assertTrue(Durations.beforeWaiting.inWholeMilliseconds > 0, "пауза одна на всё приложение")
        val working = probe(ScreenState.Working).use { it.frame() }
        val blank = RenderProbe { Column(modifier = Modifier.fillMaxSize()) {} }.use { it.frame() }
        assertContentSame(blank, working)
    }

    /** Кассовая часть: журнал документов до первого ответа узла. */
    @Test
    fun `журнал кассы показывает ожидание, а не пустой список`() {
        var rows = false
        RenderProbe {
            Column(modifier = Modifier.fillMaxSize()) {
                JournalView(
                    journal = journal,
                    entries = emptyList(),
                    types = emptyList(),
                    query = JournalQuery(),
                    loading = true,
                    more = false,
                    empty = JournalEmpty(journal.emptyDay, journal.emptyDayHint),
                    onQuery = {},
                    onMore = {},
                    onOpen = { rows = true }
                )
            }
        }.use { assertTrue(it.frame().isNotEmpty()) }
        assertFalse(rows, "строк ещё нет и быть не может")
    }

    /** Кабинет: колонка точек до первого ответа кабинета. */
    @Test
    fun `колонка точек кабинета не врёт, что точек нет`() {
        RenderProbe {
            PlaceTree(
                texts = cabinet,
                language = Language.Ru,
                collapsed = false,
                onToggle = {},
                rows = emptyList(),
                total = 0,
                loading = true,
                trouble = null,
                onRetry = {},
                query = "",
                onQuery = {},
                place = null,
                register = null,
                onPlace = {},
                onRegister = {},
                footer = {}
            )
        }.use { assertTrue(it.frame().isNotEmpty()) }
    }

    private fun assertContentSame(expected: ByteArray, actual: ByteArray) {
        assertTrue(expected.contentEquals(actual), "кадры разошлись: ${expected.size} против ${actual.size}")
    }
}
