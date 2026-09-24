package kz.mybrain.superkassa.presentation.kassa.cash

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.mybrain.superkassa.presentation.shell.AppContainer

/**
 * Модель денежного ящика окна.
 *
 * Одна на окно: набранная сумма и ключ неудавшейся попытки переживают
 * уход кассира в другой раздел.
 */
@Composable
fun cashViewModel(app: AppContainer): CashViewModel = viewModel { cashModel(app) }

/** Модель со сценариями, собранными из портов окна; проверки зовут её без окна. */
fun cashModel(app: AppContainer): CashViewModel = CashViewModel(CashCases(app.kassa, app.signIn), app.talk)
