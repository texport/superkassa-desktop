package kz.mybrain.superkassa.presentation.settings.receipt

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.mybrain.superkassa.presentation.common.model.WindowServices

/** Модель печатной формы кассы окна. */
@Composable
internal fun receiptFormViewModel(services: WindowServices): ReceiptFormViewModel =
    viewModel { receiptFormModel(services) }

/** Модель со сценариями, собранными из портов окна; проверки зовут её без окна. */
fun receiptFormModel(services: WindowServices): ReceiptFormViewModel =
    ReceiptFormViewModel(ReceiptFormCases(services.kassa, services.signIn), services.talk)
