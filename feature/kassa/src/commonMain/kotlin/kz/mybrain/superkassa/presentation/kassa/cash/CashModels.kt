package kz.mybrain.superkassa.presentation.kassa.cash

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.mybrain.superkassa.presentation.common.model.WindowServices

/**
 * Модель денежного ящика окна.
 *
 * Одна на окно: набранная сумма и ключ неудавшейся попытки переживают
 * уход кассира в другой раздел.
 */
@Composable
fun cashViewModel(services: WindowServices): CashViewModel = viewModel { cashModel(services) }

/** Модель со сценариями, собранными из портов окна; проверки зовут её без окна. */
fun cashModel(services: WindowServices): CashViewModel =
    CashViewModel(CashCases(services.kassa, services.signIn), services.talk)
