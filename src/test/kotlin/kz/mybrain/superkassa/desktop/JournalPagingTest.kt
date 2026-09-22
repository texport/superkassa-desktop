package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.ui.history.shownNote
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.journalTexts
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Дочитывание страницами: что сказано о прочитанном и о непрочитанном.
 *
 * Узел отдаёт журнал и смены страницами по двести. Оборванный на двухсотой
 * список — обычное дело у оживлённой кассы, и всё, что о нём сказано,
 * владелец принимает за факт: «показано 200 / 200» он читает как весь
 * день, а «показаны все смены» — как всю историю кассы.
 */
class JournalPagingTest {

    private val texts = journalTexts(Language.Ru)

    @Test
    fun `шапка журнала не выдаёт прочитанное за весь срок`() {
        val journal = texts.history

        val whole = shownNote(journal, shown = 12, rows = 60, more = false)
        val part = shownNote(journal, shown = 200, rows = 200, more = true)

        assertEquals("${journal.shown}: 12 / 60", whole)
        assertTrue(
            !part.startsWith("${journal.shown}:"),
            "«${journal.shown}: 200 / 200» над кнопкой «${journal.showMore}» читается как весь срок"
        )
        assertEquals("${journal.shownOfRead}: 200 / 200", part)
    }
}
