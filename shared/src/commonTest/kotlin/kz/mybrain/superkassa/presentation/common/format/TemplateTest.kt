package kz.mybrain.superkassa.presentation.common.format

import kz.mybrain.superkassa.presentation.strings.common.Language
import kz.mybrain.superkassa.presentation.strings.kassa.moneyTexts
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Подстановка в шаблон надписи.
 *
 * Место значения у языков разное, и подстановка идёт по порядку мест,
 * а не по месту в строке: «Внести %s?» и «%s салынсын ба?» получают
 * одну и ту же сумму.
 */
class TemplateTest {

    @Test
    fun `значение встаёт на место знака`() {
        assertEquals("Внести 500,00 ₸?", "Внести %s?".fill("500,00 ₸"))
        assertEquals("500,00 ₸ салынсын ба?", "%s салынсын ба?".fill("500,00 ₸"))
    }

    @Test
    fun `значения встают по порядку мест`() {
        assertEquals("3 из 12", "%s из %s".fill(3, 12))
        assertEquals("В смене документов: 4, в ящике 0", "В смене документов: %s, в ящике %s".fill("4", 0))
    }

    @Test
    fun `шаблон без мест остаётся как есть`() {
        assertEquals("Месяц", "Месяц".fill("лишнее"))
    }

    @Test
    fun `недостающее значение оставляет место видимым`() {
        assertEquals("1 из %s", "%s из %s".fill(1))
    }

    /** Сумма в надписи одна и та же на трёх языках: разряды, запятая, знак тенге. */
    @Test
    fun `сумма встаёт в надпись на каждом языке`() {
        val amount = Money.formatTiyn(123_456_789L)
        val asked = Language.entries.associateWith { moneyTexts(it).drawer.confirmDeposit.fill(amount) }
        assertEquals("Внести 1\u00A0234\u00A0567,89\u00A0₸?", asked[Language.Ru])
        assertEquals("1\u00A0234\u00A0567,89\u00A0₸ салынсын ба?", asked[Language.Kk])
        assertEquals("Pay in 1\u00A0234\u00A0567,89\u00A0₸?", asked[Language.En])
    }
}
