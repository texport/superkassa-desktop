package kz.mybrain.superkassa.desktop.ui.history

import java.time.LocalDate
import java.time.ZoneId

/** Сутки, за которые узел отдаёт журнал: от начала дня до начала следующего. */
data class DayRange(val fromMillis: Long, val toMillis: Long)

/**
 * Границы выбранного дня по часам этой машины.
 *
 * День берётся местный, а не по Гринвичу: кассир ищет свои вчерашние чеки,
 * а не отрезок суток, сдвинутый на часовой пояс сервера.
 */
fun dayRange(day: LocalDate, zone: ZoneId = ZoneId.systemDefault()): DayRange = DayRange(
    fromMillis = day.atStartOfDay(zone).toInstant().toEpochMilli(),
    toMillis = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
)
