package kz.mybrain.superkassa.domain.kassa.model.sale

import io.github.texport.superkassa.core.presentation.api.model.common.VatRateResponse
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import kz.mybrain.superkassa.domain.kassa.model.paysVat

/** Ставка по умолчанию: касса не додумывает налог за кассира. */
const val NO_VAT: String = "NO_VAT"

/**
 * Коды запасного перечня ставок: пока справочник кассы не прочитан,
 * пустое поле ставки хуже неполного.
 */
val FALLBACK_VAT_CODES: List<String> = listOf(NO_VAT, "VAT_0", "VAT_5", "VAT_10", "VAT_16")

/**
 * Ставка, с которой начинается новая позиция.
 *
 * Берётся из настроек кассы: у плательщика НДС каждая позиция облагается
 * по ставке кассы, и подставлять «Без НДС» значит занизить налог в чеке.
 * Своей ставки у кассы может не быть, её может не знать справочник или
 * не допускать режим — тогда остаётся «Без НДС».
 */
fun defaultVatOf(kkm: KkmResponse?, read: List<VatRateResponse>): String =
    kkm?.defaultVatGroup?.takeIf { it in vatCodesOf(kkm, read) } ?: NO_VAT

/** Коды ставок, которые касса примет у позиции: справочник в пределах режима. */
fun vatCodesOf(kkm: KkmResponse?, read: List<VatRateResponse>): List<String> =
    if (paysVat(kkm)) read.map { it.code }.ifEmpty { FALLBACK_VAT_CODES } else listOf(NO_VAT)
