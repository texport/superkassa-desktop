package kz.mybrain.superkassa.strings.impl.kassa.refusal

import kz.mybrain.superkassa.strings.api.kassa.refusal.KassaRefusalTexts

/** Свои слова для отказа кассы с этим кодом; `null` — своих слов у кода нет. */
internal fun kassaRefusalOwnWords(texts: KassaRefusalTexts, code: String): String? = OWN_WORDS[code]?.invoke(texts)

/**
 * Код отказа, который приложение ставит само, до кассы: ставка НДС, которой
 * ядро не знает. Своих слов у такого отказа нет — только эти.
 *
 * Значение совпадает с кодом домена приложения; расхождение ловит проверка
 * слов отказа в приложении.
 */
private const val UNKNOWN_VAT = "VAT_GROUP_UNKNOWN"

/** Коды, слова ядра на которых кассиру не годятся, — и свои слова для них. */
private val OWN_WORDS: Map<String, (KassaRefusalTexts) -> String> = mapOf(
    "KKM_IN_PROGRAMMING" to { it.programming },
    "RECEIPT_VAT_NOT_ALLOWED" to { it.vatNotPayer },
    UNKNOWN_VAT to { it.vatUnknown },
    "PAYMENT_TYPE_NOT_SUPPORTED" to { it.paymentUnsupported },
    "MEASURE_UNIT_CODE_INVALID" to { it.unitUnknown },
    "RECEIPT_VALUE_OUT_OF_RANGE" to { it.outOfRange },
    "PARENT_TICKET_REQUIRED" to { it.basisRequired }
)
