package kz.mybrain.superkassa.desktop.ui.cabinet

import kz.mybrain.superkassa.desktop.ui.components.Money
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Время и суммы кабинета в том же виде, что и во всём приложении.
 *
 * Кабинет отдаёт время строкой ISO-8601 в UTC — `2026-09-07T19:42:08.434170Z`.
 * Показывать её как есть значит требовать от владельца пересчёта часов
 * в уме; в журналах кассы то же самое время уже давно печатается местным
 * и по-человечески.
 */
fun cabinetMoment(iso: String?): String {
    val value = iso?.takeIf { it.isNotBlank() } ?: return Glyphs.DASH
    return runCatching { MOMENT.format(Instant.parse(value)) }.getOrDefault(value)
}

/** Только день: для регистрационной карты и заявлений час не нужен. */
fun cabinetDay(iso: String?): String {
    val value = iso?.takeIf { it.isNotBlank() } ?: return Glyphs.DASH
    return runCatching { DAY.format(Instant.parse(value)) }.getOrDefault(value)
}

/**
 * Сумма кабинета словами кассира: разряды разделены, копеек не бывает —
 * бывают тиыны.
 */
fun cabinetSum(value: BigDecimal?): String = value?.let { Money.format(it) } ?: Glyphs.DASH

/**
 * Количество товара в строке чека.
 *
 * Незначащие нули отброшены: кабинет отдаёт количество с тремя знаками
 * после запятой, и «2,000 × 450,00 ₸» читается как две тысячи штук.
 */
fun cabinetQuantity(value: BigDecimal?): String =
    value?.stripTrailingZeros()?.toPlainString() ?: Glyphs.DASH

private val MOMENT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault())

private val DAY: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZoneId.systemDefault())
