package kz.mybrain.superkassa.presentation.kassa.sale.position

import androidx.compose.runtime.staticCompositionLocalOf
import io.github.texport.superkassa.core.presentation.api.model.common.VatRateResponse
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import kz.mybrain.superkassa.designsystem.theme.icon.Glyphs
import kz.mybrain.superkassa.domain.kassa.model.paysVat
import kz.mybrain.superkassa.domain.kassa.model.sale.FALLBACK_VAT_CODES
import kz.mybrain.superkassa.domain.kassa.model.sale.NO_VAT
import kz.mybrain.superkassa.presentation.words.common.of
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.common.EnumStrings

/**
 * Ставка НДС: код уходит в чек, название и величина видны кассиру.
 *
 * Величина обязательна на экране: кассир пробивает чек и отвечает за
 * налог в нём, а «НДС» без числа не говорит, по какой ставке. В ОФД она
 * уходит в тысячных долях процента, и это преобразование делает касса —
 * касса величину только показывает.
 */
data class VatRate(val code: String, val title: String, val percent: Int? = null)

/**
 * Ставки НДС, из которых кассир выбирает у позиции.
 *
 * Пусто, когда НДС задан на весь чек: у позиций тогда ставок нет,
 * и ни поле ставки, ни ставка в строке чека не показываются.
 *
 * Перечень приходит справочником ставок кассы. Свой список здесь
 * разошёлся бы с кассой при первой же смене ставок — в 2026
 * году это уже произошло: ставка 12% ушла из основных, но осталась нужна
 * для возврата по чеку, пробитому до неё.
 */
val LocalVatRates = staticCompositionLocalOf<List<VatRate>> { emptyList() }

/**
 * Ставки, которые показывать кассиру.
 *
 * Перечень — справочник ставок кассы. Пока он не прочитан, берётся
 * запасной перечень: пустое поле ставки хуже неполного.
 *
 * Налоговый режим кассы решает, есть ли выбор вообще. Неплательщик НДС
 * налога не выделяет: касса такую позицию отвергает, а прежде принимала
 * и отбрасывала ставку молча — кассир видел и печатал «НДС 12%»,
 * а в ОФД уходила позиция без налога.
 */
fun vatRatesOf(read: List<VatRateResponse>, kkm: KkmResponse?, language: Language, texts: EnumStrings): List<VatRate> {
    val rates = read.map { VatRate(it.code, it.name.of(language) ?: it.code, it.percent) }
        .ifEmpty { fallbackRates(texts) }
    if (paysVat(kkm)) return rates
    return rates.filter { it.code == NO_VAT }.ifEmpty { listOf(VatRate(NO_VAT, texts.vatNone)) }
}

/**
 * Название ставки по коду; неизвестный код показывается как есть.
 *
 * Величина добавляется к названию, только когда её в названии ещё нет:
 * касса называет ставку «НДС 16%», и «НДС 16% · 16%» кассир читает как
 * две разные ставки подряд.
 *
 * «Без НДС» остаётся без величины, даже когда касса присылает при нём ноль:
 * «Без НДС 0%» читается как нулевая ставка налога, а это другое обложение
 * и другая строка чека, чем товар вне НДС.
 */
fun vatTitle(rates: List<VatRate>, code: String): String {
    val rate = rates.firstOrNull { it.code == code } ?: return code
    if (rate.code == NO_VAT) return rate.title
    val percent = rate.percent ?: return rate.title
    return if (rate.title.contains(percent.toString())) rate.title else "${rate.title} $percent${Glyphs.PERCENT}"
}

/** Запасной перечень, пока справочник кассы не прочитан: коды — те же, что знает правило ставок. */
private fun fallbackRates(texts: EnumStrings): List<VatRate> =
    FALLBACK_VAT_CODES.zip(listOf(texts.vatNone, texts.vat0, texts.vat5, texts.vat10, texts.vat16), ::VatRate)
