package kz.mybrain.superkassa.presentation.kassa.refund

import kz.mybrain.superkassa.domain.kassa.model.ReturnDay
import kz.mybrain.superkassa.domain.kassa.model.TillReference
import kz.mybrain.superkassa.domain.kassa.model.sale.DomainKind

/** Справочники кассы в состоянии экрана. */
val ReturnsUiState.reference: TillReference get() = TillReference(paymentTypes, vatRates)

fun ReturnsUiState.withReference(reference: TillReference): ReturnsUiState =
    copy(paymentTypes = reference.paymentTypes, vatRates = reference.vatRates)

/**
 * Принимает перечитанный день: документы, смену, остаток и отрасль.
 *
 * Смену, о которой касса не сказала, берёт из сведений о кассе: выдумывать
 * закрытую смену хуже, чем показать устаревший признак.
 */
fun ReturnsUiState.adopt(day: ReturnDay): ReturnsUiState = copy(
    shiftOpen = day.shiftOpen ?: (kkm?.isShiftOpen == true),
    cashInDrawer = day.cash,
    domainKind = DomainKind.byCode(day.domainCode),
    documents = day.documents ?: documents,
    dayRead = day.documents != null,
    loading = false
)
