package kz.mybrain.superkassa.designsystem.format

import kotlinx.datetime.LocalDate
import kz.mybrain.superkassa.designsystem.theme.icon.Glyphs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Даты и время на экране.
 *
 * Время показывается по поясу кассы, `Asia/Almaty`, какой бы пояс ни стоял
 * на машине: 14:42 по Гринвичу — это 19:42 в Казахстане. Вид цифровой,
 * и он один на казахском, русском и английском.
 */
class DatesTest {

    private val moment = Instant.parse("2026-09-07T14:42:08Z").toEpochMilliseconds()

    @Test
    fun `день показывается полностью`() {
        assertEquals("07.09.2026", Dates.day(LocalDate(2026, 9, 7)))
        assertEquals("31.12.2025", Dates.day(LocalDate(2025, 12, 31)))
    }

    @Test
    fun `подпись оси — день без года`() {
        assertEquals("07.09", Dates.dayMonth(LocalDate(2026, 9, 7)))
    }

    @Test
    fun `время документа — по поясу кассы`() {
        assertEquals("07.09.2026 19:42", Dates.moment(moment))
        assertEquals("07.09 19:42", Dates.shortMoment(moment))
        assertEquals("07.09 19:42:08", Dates.stamp(moment))
    }

    @Test
    fun `вечер по Гринвичу — уже завтра в Казахстане`() {
        val evening = Instant.parse("2026-09-07T20:15:00Z").toEpochMilliseconds()
        assertEquals("08.09.2026 01:15", Dates.moment(evening))
    }

    @Test
    fun `нет времени — прочерк`() {
        assertEquals(Glyphs.DASH, Dates.moment(null))
        assertEquals(Glyphs.DASH, Dates.shortMoment(null))
        assertEquals(Glyphs.DASH, Dates.stamp(null))
    }

    @Test
    fun `время кабинета приходит записью в UTC`() {
        assertEquals("07.09.2026 19:42", Dates.momentOf("2026-09-07T14:42:08.434170Z"))
        assertEquals("08.09.2026", Dates.dayOf("2026-09-07T20:15:00Z"))
    }

    @Test
    fun `незнакомая запись времени показывается как пришла`() {
        assertEquals("вчера", Dates.momentOf("вчера"))
        assertEquals(Glyphs.DASH, Dates.momentOf(" "))
        assertEquals(Glyphs.DASH, Dates.dayOf(null))
    }

    @Test
    fun `час графика — двумя цифрами`() {
        assertEquals("09:00", Times.hour(9))
        assertEquals("14:00", Times.hour(14))
    }

    @Test
    fun `остаток ожидания — минутами и секундами`() {
        assertEquals("2:58", Times.countdown(178.seconds))
        assertEquals("0:05", Times.countdown(5.seconds))
    }
}
