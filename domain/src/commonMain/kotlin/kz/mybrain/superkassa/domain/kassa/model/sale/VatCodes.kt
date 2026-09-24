package kz.mybrain.superkassa.domain.kassa.model.sale

import io.github.texport.superkassa.core.presentation.api.model.common.VatRateResponse
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import io.github.texport.superkassa.core.presentation.api.model.kkm.VatGroup
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
internal fun vatCodesOf(kkm: KkmResponse?, read: List<VatRateResponse>): List<String> =
    if (paysVat(kkm)) read.map { it.code }.ifEmpty { FALLBACK_VAT_CODES } else listOf(NO_VAT)

/**
 * Код отказа, который касса приложения ставит сама, до ядра: в чеке ставка
 * НДС, которой нет в перечне ставок ядра.
 *
 * Ядро на такую ставку отвечает не отказом, а сбоем разбора запроса,
 * и кассир читал бы «касса не ответила — проверьте журнал», хотя чек
 * не пробивался. Слова отказа даёт экран по этому коду.
 */
const val UNKNOWN_VAT: String = "VAT_GROUP_UNKNOWN"

/** Первая ставка, которой нет в перечне ставок ядра; `null` — все знакомы. */
internal fun unknownVatIn(codes: List<String>): String? =
    codes.firstOrNull { code -> VatGroup.entries.none { it.name == code } }
