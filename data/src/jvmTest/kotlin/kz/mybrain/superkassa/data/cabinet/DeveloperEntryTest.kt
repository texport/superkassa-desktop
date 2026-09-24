package kz.mybrain.superkassa.data.cabinet

import kz.mybrain.superkassa.integrations.bfdcabinet.DevelopmentIdentity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Вход разработчика включается только явно: обе переменные и обе — по
 * двенадцать цифр. Иначе — вход по ЭЦП.
 */
class DeveloperEntryTest {

    private fun entry(iin: String?, bin: String?) = DeveloperEntry.fromEnvironment { name ->
        when (name) {
            DeveloperEntry.IIN_VARIABLE -> iin
            DeveloperEntry.BIN_VARIABLE -> bin
            else -> null
        }
    }

    @Test
    fun `без настройки вход по ЭЦП`() {
        assertNull(entry(null, null))
    }

    @Test
    fun `обе переменные по двенадцать цифр включают вход разработчика`() {
        assertEquals(DevelopmentIdentity("920313351246", "180140000123"), entry(" 920313351246 ", "180140000123"))
    }

    @Test
    fun `одной переменной или опечатки мало`() {
        assertNull(entry("920313351246", null))
        assertNull(entry(null, "180140000123"))
        assertNull(entry("92031335124", "180140000123"))
        assertNull(entry("920313351246", "18014000012X"))
    }
}
