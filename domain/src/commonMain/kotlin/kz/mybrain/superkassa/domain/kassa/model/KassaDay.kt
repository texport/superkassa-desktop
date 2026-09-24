package kz.mybrain.superkassa.domain.kassa.model

import io.github.texport.superkassa.core.presentation.api.model.common.VatRateResponse
import io.github.texport.superkassa.core.presentation.api.model.kkm.FiscalDocumentResponse
import io.github.texport.superkassa.core.presentation.api.model.reference.PaymentTypeResponse

/**
 * Справочники кассы, нужные продаже и возврату: виды оплаты и ставки НДС.
 * Пустой список — справочник не прочитан, и экран берёт свой запасной.
 */
data class TillReference(
    val paymentTypes: List<PaymentTypeResponse> = emptyList(),
    val vatRates: List<VatRateResponse> = emptyList()
)

/**
 * Что продаже нужно знать о выбранной кассе сейчас.
 *
 * @property shiftOpen открыта ли смена со слов кассы.
 * @property domainCode код вида отрасли, выбранный на рабочем месте; `null` — торговля.
 */
data class SaleSeat(val shiftOpen: Boolean, val domainCode: String?)

/**
 * Денежный ящик за одно перечитывание.
 *
 * @property shiftOpen открыта ли смена; `null` — касса не сказала.
 * @property cash наличные в ящике, в тиынах; `null` — касса не сказала.
 * @property recent внесения и изъятия за сутки, новые первыми; `null` — не прочитаны.
 * @property trouble первая беда чтения; `null` — прочиталось всё.
 */
data class DrawerDay(
    val shiftOpen: Boolean?,
    val cash: Long?,
    val recent: List<FiscalDocumentResponse>?,
    val trouble: Answer<Nothing>?
)

/**
 * День кассы для возврата: документы дня, смена, ящик и отрасль.
 *
 * @property documents документы дня, все, новые первыми; `null` — касса их не отдала.
 * @property trouble первая беда чтения; `null` — прочиталось всё.
 */
data class ReturnDay(
    val shiftOpen: Boolean?,
    val cash: Long?,
    val domainCode: String?,
    val documents: List<FiscalDocumentResponse>?,
    val trouble: Answer<Nothing>?
)

/** Беда ответа, если она есть: отказ или сбой; удача бедой не считается. */
internal fun Answer<*>.trouble(): Answer<Nothing>? = when (this) {
    is Answer.Done -> null
    is Answer.Refused -> this
    is Answer.Failed -> this
}
