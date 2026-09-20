package kz.mybrain.superkassa.desktop.ui.sale

import androidx.compose.runtime.staticCompositionLocalOf
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.Dictionary
import kz.mybrain.superkassa.desktop.ui.strings.EnumStrings

/**
 * Ставка НДС: код уходит в чек, название и величина видны кассиру.
 *
 * Величина обязательна на экране: кассир пробивает чек и отвечает за
 * налог в нём, а «НДС» без числа не говорит, по какой ставке. В ОФД она
 * уходит в тысячных долях процента, и это преобразование делает узел —
 * касса величину только показывает.
 */
data class VatRate(val code: String, val title: String, val percent: Int? = null)

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
 *
 * Налоговый режим кассы решает, есть ли выбор вообще. Неплательщик НДС
 * налога не выделяет: узел такую позицию отвергает, а до отказа её
 * принимал и отбрасывал ставку молча — кассир видел и печатал «НДС 12%»,
 * а в ОФД уходила позиция без налога.
 */
fun vatRatesOf(session: Session, texts: EnumStrings): List<VatRate> {
    val rates = knownRates(session, texts)
    if (vatAllowed(session)) return rates
    return rates.filter { it.code == NO_VAT }.ifEmpty { listOf(VatRate(NO_VAT, texts.vatNone)) }
}

/**
 * Допускает ли режим кассы ставки НДС.
 *
 * Режим не назван — ставки остаются: у плательщика НДС спрятать их
 * значит занизить налог в чеке, а несовпадение со старой кассой узел
 * теперь называет отказом, а не проглатывает.
 */
fun vatAllowed(session: Session): Boolean = session.selected?.taxRegime != NO_VAT_REGIME

/** Перечень ставок в том виде, в каком его знает узел. */
private fun knownRates(session: Session, texts: EnumStrings): List<VatRate> {
    // Величину ставки отдаёт только метод ставок: в справочнике групп
    // лежат код и название, и по ним кассир не узнает, сколько процентов.
    val withPercent = session.vatRates
    if (withPercent.isNotEmpty()) {
        return withPercent.map { VatRate(it.code, it.name.title(session.language.code), it.percent) }
    }
    val fromNode = session.dictionaries[Dictionary.VatGroups].orEmpty()
    if (fromNode.isEmpty()) return fallbackRates(texts)
    return fromNode.map { VatRate(it.code, it.title(session.language.code)) }
}

/**
 * Название ставки по коду; неизвестный код показывается как есть.
 *
 * Величина добавляется к названию, только когда её в названии ещё нет:
 * узел называет ставку «НДС 16%», и «НДС 16% · 16%» кассир читает как
 * две разные ставки подряд.
 */
fun vatTitle(rates: List<VatRate>, code: String): String {
    val rate = rates.firstOrNull { it.code == code } ?: return code
    val percent = rate.percent ?: return rate.title
    return if (rate.title.contains(percent.toString())) rate.title else "${rate.title} $percent%"
}

/** Ставка по умолчанию: касса не додумывает налог за кассира. */
const val NO_VAT = "NO_VAT"

/** Налоговый режим кассы, при котором НДС в чеке не выделяется. */
const val NO_VAT_REGIME = "NO_VAT"

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
