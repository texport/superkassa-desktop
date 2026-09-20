package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.ui.strings.JournalTexts
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.journalTexts
import kz.mybrain.superkassa.desktop.ui.strings.journalTextsEn
import kz.mybrain.superkassa.desktop.ui.strings.journalTextsKk
import kz.mybrain.superkassa.desktop.ui.strings.journalTextsRu
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

    /** Все надписи области одной картой: имя поля к тексту. */
    private fun values(texts: JournalTexts): Map<String, String> = buildMap {
        putAll(strings("returns", texts.returns))
        putAll(strings("history", texts.history))
        putAll(strings("shifts", texts.shifts))
        putAll(strings("queue", texts.queue))
    }

    private fun strings(group: String, holder: Any): Map<String, String> =
        holder.javaClass.declaredFields
            .filter { it.type == String::class.java }
            .associate { field ->
                field.isAccessible = true
                "$group.${field.name}" to (field.get(holder) as String)
            }

    @Test
    fun `ни одна надпись не пуста ни на одном языке`() {
        Language.entries.forEach { language ->
            values(journalTexts(language)).forEach { (name, text) ->
                assertTrue(text.isNotBlank(), "$name пуст на ${language.code}")
            }
        }
    }

    @Test
    fun `каждая надпись переведена, а не скопирована`() {
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
            val texts = journalTexts(language)
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
        assertEquals(journalTextsKk, journalTexts(Language.Kk))
        assertEquals(journalTextsRu, journalTexts(Language.Ru))
        assertEquals(journalTextsEn, journalTexts(Language.En))
    }
}
