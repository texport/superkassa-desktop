package kz.mybrain.superkassa.strings.api.journal

import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Отказ БФД — словами кассира.
 *
 * БФД отвечает кодом и английским пояснением: «Same customer and taxpayer
 * IIN» стояло на экране кассира, который решает, что делать с чеком.
 * Знакомый код переводится, незнакомый остаётся пояснением службы —
 * молчать об отказе хуже, чем сказать о нём чужими словами.
 */
class OfdRefusalWordsTest {

    private fun refusal(language: Language) = textsOf(language).journal.ofdRefusal

    @Test
    fun `известный код переводится на язык кассира`() {
        assertEquals(refusal(Language.Ru).sameTaxpayer, refusal(Language.Ru).words(17))
        assertEquals(refusal(Language.Kk).notEnoughCash, refusal(Language.Kk).words(14))
        assertEquals(refusal(Language.En).invalidToken, refusal(Language.En).words(2))
    }

    /** Код, которого приложение не знает, слов не выдумывает. */
    @Test
    fun `незнакомый код слов не получает`() {
        assertNull(refusal(Language.Ru).words(7))
        assertNull(refusal(Language.Ru).words(null))
    }
}
