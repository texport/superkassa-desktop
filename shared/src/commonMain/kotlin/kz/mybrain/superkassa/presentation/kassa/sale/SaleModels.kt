package kz.mybrain.superkassa.presentation.kassa.sale

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.mybrain.superkassa.presentation.shell.AppContainer

/**
 * Модель продажи окна.
 *
 * Одна на окно: живёт в хранилище моделей окна, и корзина переживает
 * уход кассира в другой раздел. Проверки создают модель сами, без окна.
 */
@Composable
fun saleViewModel(app: AppContainer): SaleViewModel = viewModel { saleModel(app) }

/** Модель со сценариями, собранными из портов окна; проверки зовут её без окна. */
fun saleModel(app: AppContainer): SaleViewModel =
    SaleViewModel(SaleCases(app.kassa, app.signIn, app.memory), app.talk)
