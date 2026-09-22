package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.blockReasonTexts
import kz.mybrain.superkassa.desktop.ui.strings.blockReasonWords
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Причина блокировки называется своими словами.
 *
 * Прежде на месте причины стояла одна фраза на все случаи, и она же
 * говорила про снятие с учёта: кассир с отозванным токеном читал, что
 * его кассу сняли с учёта, и шёл разбираться не туда.
 */
class BlockReasonWordsTest {

    @Test
    fun `известная причина названа, незнакомая остаётся общей`() {
        val ru = blockReasonTexts(Language.Ru)
        assertEquals(ru.invalidToken, blockReasonWords(1002, Language.Ru))
        assertEquals(ru.shiftTooLong, blockReasonWords(1011, Language.Ru))
        assertEquals(ru.autonomousTooLong, blockReasonWords(2001, Language.Ru))
        assertEquals(ru.deregistered, blockReasonWords(1018, Language.Ru))
        assertEquals(ru.unknown, blockReasonWords(777, Language.Ru))
        assertEquals(ru.unknown, blockReasonWords(null, Language.Ru))
        assertEquals(blockReasonTexts(Language.Kk).invalidToken, blockReasonWords(1002, Language.Kk))
    }
}
