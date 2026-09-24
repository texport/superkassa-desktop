package kz.mybrain.superkassa.integrations.bfdcabinet

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

/**
 * Числа кабинета читаются записью, а не двоичной дробью.
 *
 * Десяти тиынов в `Double` не существует: `0.1 + 0.2` — не `0.3`, и сумма
 * смены, собранная так, расходилась бы с кассой на тиын.
 */
class CabinetDecimalTest {

    @Test
    fun notationIsKeptAndExponentUnfolded() {
        assertEquals("690.00", CabinetDecimal.of("690.00").plain)
        assertEquals("1000", CabinetDecimal.of("1E+3").plain)
        assertEquals("0.0125", CabinetDecimal.of("12.5e-3").plain)
        assertEquals("0.5", CabinetDecimal.of(".5").plain)
        assertEquals("-0.40", CabinetDecimal.of("-0.40").plain)
        assertEquals("0.00", CabinetDecimal.of("-0.00").plain)
    }

    @Test
    fun equalityIsByValue() {
        assertEquals(CabinetDecimal.of("690.00"), CabinetDecimal.of("690"))
        assertEquals(CabinetDecimal.of("690.00").hashCode(), CabinetDecimal.of("690").hashCode())
        assertNotEquals(CabinetDecimal.of("690.01"), CabinetDecimal.of("690"))
    }

    /** Больше двух знаков кабинет не шлёт; пришлёт — тиын округляется половиной от нуля. */
    @Test
    fun tengeInTiynRoundsHalfAwayFromZero() {
        assertEquals(1250L, CabinetDecimal.of("12.5").tiyn())
        assertEquals(1235L, CabinetDecimal.of("12.345").tiyn())
        assertEquals(-1235L, CabinetDecimal.of("-12.345").tiyn())
        assertEquals(1234L, CabinetDecimal.of("12.344").tiyn())
        assertEquals(100_000L, CabinetDecimal.of("1E+3").tiyn())
    }

    @Test
    fun notANumberIsRefused() {
        assertFailsWith<IllegalArgumentException> { CabinetDecimal.of("двенадцать") }
        assertFailsWith<IllegalArgumentException> { CabinetDecimal.of(".") }
        assertFailsWith<IllegalArgumentException> { CabinetDecimal.of("") }
    }

    /** Из числа и из строки читается одинаково, а пишется числом той же записью. */
    @Test
    fun jsonReadsNumberOrStringAndWritesNumber() {
        val serializer = ListSerializer(CabinetDecimal.serializer())
        val read = Json.decodeFromString(serializer, """[61825.00, "0.10", 1E+3]""")

        assertEquals(listOf("61825.00", "0.10", "1000"), read.map { it.plain })
        assertEquals("""[61825.00,0.10,1000]""", Json.encodeToString(serializer, read))
    }
}
