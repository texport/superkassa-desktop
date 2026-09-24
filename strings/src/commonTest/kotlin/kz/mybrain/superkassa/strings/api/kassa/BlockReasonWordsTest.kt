package kz.mybrain.superkassa.strings.api.kassa

import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
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
    fun `известная причина названа — незнакомая остаётся общей`() {
        val ru = textsOf(Language.Ru).kassa.blockReason
        assertEquals(ru.invalidToken, ru.words(1002))
        assertEquals(ru.shiftTooLong, ru.words(1011))
        assertEquals(ru.autonomousTooLong, ru.words(2001))
        assertEquals(ru.deregistered, ru.words(1018))
        assertEquals(ru.unknown, ru.words(777))
        assertEquals(ru.unknown, ru.words(null))
        val kk = textsOf(Language.Kk).kassa.blockReason
        assertEquals(kk.invalidToken, kk.words(1002))
    }
}
