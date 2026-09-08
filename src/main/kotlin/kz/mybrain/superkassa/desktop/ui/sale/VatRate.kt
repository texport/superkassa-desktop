package kz.mybrain.superkassa.desktop.ui.sale

import androidx.compose.runtime.staticCompositionLocalOf
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.Dictionary
import kz.mybrain.superkassa.desktop.ui.strings.EnumStrings

/** Ставка НДС: код уходит в чек, название видит кассир. */
data class VatRate(val code: String, val title: String)

/**
 * Ставки НДС кассы.
 *
 * Перечень приходит справочником узла `/dictionaries/vat-groups`. Свой
 * список здесь разошёлся бы с узлом при первой же смене ставок — в 2026
 * году это уже произошло: ставка 12% ушла из основных, но осталась нужна
 * для возврата по чеку, пробитому до неё.
 */
val LocalVatRates = staticCompositionLocalOf<List<VatRate>> { emptyList() }

/**
 * Ставки, которые показывать кассиру.
 *
 * Пока справочник не прочитан, берётся запасной перечень: пустое поле
 * ставки хуже неполного, а без связи с узлом чек всё равно не пробить.
 */
fun vatRatesOf(session: Session, texts: EnumStrings): List<VatRate> {
    val fromNode = session.dictionaries[Dictionary.VatGroups].orEmpty()
    if (fromNode.isEmpty()) return fallbackRates(texts)
    return fromNode.map { VatRate(it.code, it.title(session.language.code)) }
}

/** Название ставки по коду; неизвестный код показывается как есть. */
fun vatTitle(rates: List<VatRate>, code: String): String =
    rates.firstOrNull { it.code == code }?.title ?: code

/** Ставка по умолчанию: касса не додумывает налог за кассира. */
const val NO_VAT = "NO_VAT"

private fun fallbackRates(texts: EnumStrings): List<VatRate> = listOf(
    VatRate(NO_VAT, texts.vatNone),
    VatRate("VAT_0", texts.vat0),
    VatRate("VAT_5", texts.vat5),
    VatRate("VAT_10", texts.vat10),
    VatRate("VAT_16", texts.vat16)
)

/**
 * Ставка, с которой начинается новая позиция.
 *
 * Берётся из настроек кассы: у плательщика НДС каждая позиция облагается
 * по ставке кассы, и подставлять «Без НДС» значит занизить налог в чеке.
 * Своей ставки у кассы может не быть — тогда остаётся «Без НДС».
 */
fun defaultVatOf(session: Session, rates: List<VatRate>): String =
    session.selected?.defaultVatGroup?.takeIf { code -> rates.any { it.code == code } } ?: NO_VAT
