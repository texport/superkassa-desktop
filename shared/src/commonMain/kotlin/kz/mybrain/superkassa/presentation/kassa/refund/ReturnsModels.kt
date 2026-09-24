package kz.mybrain.superkassa.presentation.kassa.refund

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.mybrain.superkassa.presentation.shell.AppContainer

/**
 * Модель возврата окна.
 *
 * Одна на окно: выбранный чек и набранная сумма переживают уход кассира
 * в другой раздел.
 */
@Composable
fun returnsViewModel(app: AppContainer): ReturnsViewModel = viewModel { returnsModel(app) }

/** Действия экрана, выполняемые этой моделью. */
fun ReturnsViewModel.actions(): ReturnsActions = ReturnsActions(this, refund, payments)

/** Модель со сценариями, собранными из портов окна; проверки зовут её без окна. */
fun returnsModel(app: AppContainer): ReturnsViewModel =
    ReturnsViewModel(RefundCases(app.kassa, app.signIn, app.memory, app.areas.kassa), app.talk)
