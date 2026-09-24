package kz.mybrain.superkassa.strings.api.journal

import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kz.mybrain.superkassa.strings.impl.journal.journalTextsEn
import kz.mybrain.superkassa.strings.impl.journal.journalTextsKk
import kz.mybrain.superkassa.strings.impl.journal.journalTextsRu
import kz.mybrain.superkassa.strings.lines
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Надписи области возврата, истории и очереди.
 *
 * Пустые состояния этих экранов состоят из строки и подсказки: строка
 * говорит, что именно пусто, подсказка — что с этим делать. Забытая
 * подсказка оставляет кассира перед пустым экраном без объяснения,
 * а строка, скопированная в подсказку, повторяет ему одно и то же дважды.
 */
class JournalTextsTest {

    /** Надписи возврата, истории, смен и очереди одной картой: путь поля к тексту. */
    private fun values(texts: JournalTexts): Map<String, String> = buildMap {
        putAll(lines(texts.returns).map { "returns.${it.first}" to it.second })
        putAll(lines(texts.history).map { "history.${it.first}" to it.second })
        putAll(lines(texts.shifts).map { "shifts.${it.first}" to it.second })
        putAll(lines(texts.queue).map { "queue.${it.first}" to it.second })
    }

    @Test
    fun `ни одна надпись не пуста ни на одном языке`() {
        Language.entries.forEach { language ->
            values(textsOf(language).journal).forEach { (name, text) ->
                assertTrue(text.isNotBlank(), "$name пуст на ${language.code}")
            }
        }
    }

    @Test
    fun `каждая надпись переведена — а не скопирована`() {
        val ru = values(journalTextsRu)
        val kk = values(journalTextsKk)
        val en = values(journalTextsEn)
        ru.forEach { (name, text) ->
            assertTrue(text != kk[name], "$name не переведён на казахский")
            assertTrue(text != en[name], "$name не переведён на английский")
        }
    }

    @Test
    fun `подсказка пустого состояния не повторяет его строку`() {
        Language.entries.forEach { language ->
            val texts = textsOf(language).journal
            val pairs = listOf(
                "history.emptyDay" to (texts.history.emptyDay to texts.history.emptyDayHint),
                "history.emptyForFilter" to (texts.history.emptyForFilter to texts.history.emptyForFilterHint),
                "shifts.none" to (texts.shifts.none to texts.shifts.noneHint),
                "shifts.emptyDocuments" to (texts.shifts.emptyDocuments to texts.shifts.emptyDocumentsHint),
                "returns.chooseBasis" to (texts.returns.chooseBasis to texts.returns.chooseBasisHint),
                "returns.shiftClosed" to (texts.returns.shiftClosed to texts.returns.shiftClosedHint)
            )
            pairs.forEach { (name, pair) ->
                val (line, hint) = pair
                assertTrue(line != hint, "$name: подсказка повторяет строку на ${language.code}")
            }
        }
    }

    @Test
    fun `язык выбирается однозначно`() {
        assertEquals(journalTextsKk, textsOf(Language.Kk).journal)
        assertEquals(journalTextsRu, textsOf(Language.Ru).journal)
        assertEquals(journalTextsEn, textsOf(Language.En).journal)
    }
}
