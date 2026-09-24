package kz.mybrain.superkassa.presentation.common.document

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Журнал до первого ответа: общее место содержимого, а не пустой список.
 *
 * Правило самого места — у проверки `ScreenState` дизайн-системы; здесь —
 * что журнал кассы им пользуется и не рисует строк, которых ещё нет.
 */
class JournalWaitingTest {

    private val journal = textsOf(Language.Ru).journal.history

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
}
