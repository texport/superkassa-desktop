package kz.mybrain.superkassa.presentation.settings.receipt

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.mybrain.superkassa.presentation.shell.AppContainer

/** Модель печатной формы кассы окна. */
@Composable
fun receiptFormViewModel(app: AppContainer): ReceiptFormViewModel = viewModel { receiptFormModel(app) }

/** Модель со сценариями, собранными из портов окна; проверки зовут её без окна. */
fun receiptFormModel(app: AppContainer): ReceiptFormViewModel =
    ReceiptFormViewModel(ReceiptFormCases(app.kassa, app.signIn), app.talk)
