package kz.mybrain.superkassa.presentation.common.format

import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.fill
import kz.mybrain.superkassa.strings.api.textsOf
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Сумма в надписи: запись суммы приложения встаёт в шаблон текстов.
 *
 * Шаблон — у модуля текстов, запись суммы — у приложения; сходятся они
 * здесь, и место значения у языков разное: «Внести %s?» и «%s салынсын ба?».
 */
class AmountTextTest {

    /** Сумма в надписи одна и та же на трёх языках: разряды, запятая, знак тенге. */
    @Test
    fun `сумма встаёт в надпись на каждом языке`() {
        val amount = Money.formatTiyn(123_456_789L)
        val asked = Language.entries.associateWith { textsOf(it).kassa.money.drawer.confirmDeposit.fill(amount) }
        assertEquals("Внести 1 234 567,89 ₸?", asked[Language.Ru])
        assertEquals("1 234 567,89 ₸ салынсын ба?", asked[Language.Kk])
        assertEquals("Pay in 1 234 567,89 ₸?", asked[Language.En])
    }
}
