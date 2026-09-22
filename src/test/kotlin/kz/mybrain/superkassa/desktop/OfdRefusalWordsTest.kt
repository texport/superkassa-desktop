package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.ofdRefusalTexts
import kz.mybrain.superkassa.desktop.ui.strings.ofdRefusalWords
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

    @Test
    fun `известный код переводится на язык кассира`() {
        assertEquals(
            ofdRefusalTexts(Language.Ru).sameTaxpayer,
            ofdRefusalWords(17, Language.Ru)
        )
        assertEquals(
            ofdRefusalTexts(Language.Kk).notEnoughCash,
            ofdRefusalWords(14, Language.Kk)
        )
        assertEquals(
            ofdRefusalTexts(Language.En).invalidToken,
            ofdRefusalWords(2, Language.En)
        )
    }

    /** Код, которого приложение не знает, слов не выдумывает. */
    @Test
    fun `незнакомый код слов не получает`() {
        assertNull(ofdRefusalWords(7, Language.Ru))
        assertNull(ofdRefusalWords(null, Language.Ru))
    }
}
