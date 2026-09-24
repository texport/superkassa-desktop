package kz.mybrain.superkassa.presentation.common.format

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kz.mybrain.superkassa.presentation.theme.icon.Glyphs
import kotlin.time.Instant

/**
 * Даты и время на экране кассы.
 *
 * Время кассы — время Казахстана, `Asia/Almaty`, как у ядра: чек, журнал
 * и отчёт показывают один и тот же час, на какой бы машине ни открыли
 * кассу. Вид цифровой и одинаковый на казахском, русском и английском:
 * «07.09.2026 19:42», — слов в нём нет, и переводить нечего.
 *
 * Виды объявлены здесь один раз: десять своих шаблонов по экранам
 * расходились в мелочах — где с секундами, где без года.
 */
object Dates {

    /** Пояс кассы — тот же, в котором ядро пишет время чеков. */
    val KKM_ZONE: TimeZone = TimeZone.of("Asia/Almaty")

    /** День полностью: «07.09.2026». */
    fun day(date: LocalDate): String = DAY.format(date)

    /** День без года — подпись оси, где год один на весь график: «07.09». */
    fun dayMonth(date: LocalDate): String = DAY_MONTH.format(date)

    /** Время документа с минутой: «07.09.2026 19:42»; нет времени — прочерк. */
    fun moment(millis: Long?): String = millis?.let { MOMENT.format(local(it)) } ?: Glyphs.DASH

    /** Время в пределах года — строка списка, где год лишний: «07.09 19:42». */
    fun shortMoment(millis: Long?): String = millis?.let { SHORT_MOMENT.format(local(it)) } ?: Glyphs.DASH

    /** Время с секундой — журнал, где документы идут один за другим: «07.09 19:42:08». */
    fun stamp(millis: Long?): String = millis?.let { STAMP.format(local(it)) } ?: Glyphs.DASH

    /**
     * Время из записи ISO-8601, как её отдают кабинет и проверка выпусков.
     *
     * Кабинет пишет время в UTC — `2026-09-07T19:42:08.434170Z`; показывать
     * её как есть значит требовать пересчёта часов в уме. Не разобралась —
     * показывается как пришла: незнакомый вид не повод оставить строку пустой.
     */
    fun momentOf(iso: String?): String = shown(iso) { MOMENT.format(it) }

    /** Только день из записи ISO-8601: для карточек и заявлений час не нужен. */
    fun dayOf(iso: String?): String = shown(iso) { DAY.format(it.date) }

    private fun local(millis: Long): LocalDateTime = Instant.fromEpochMilliseconds(millis).toLocalDateTime(KKM_ZONE)

    private fun shown(iso: String?, format: (LocalDateTime) -> String): String {
        val value = iso?.takeIf { it.isNotBlank() } ?: return Glyphs.DASH
        return runCatching { format(Instant.parse(value).toLocalDateTime(KKM_ZONE)) }.getOrDefault(value)
    }

    private val DAY = LocalDate.Format {
        day()
        char('.')
        monthNumber()
        char('.')
        year()
    }

    private val DAY_MONTH = LocalDate.Format {
        day()
        char('.')
        monthNumber()
    }

    private val MOMENT = LocalDateTime.Format {
        date(DAY)
        char(' ')
        hour()
        char(':')
        minute()
    }

    private val SHORT_MOMENT = LocalDateTime.Format {
        date(DAY_MONTH)
        char(' ')
        hour()
        char(':')
        minute()
    }

    private val STAMP = LocalDateTime.Format {
        date(DAY_MONTH)
        char(' ')
        hour()
        char(':')
        minute()
        char(':')
        second()
    }
}
