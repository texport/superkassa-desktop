package kz.mybrain.superkassa.presentation.kassa.sale

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.mybrain.superkassa.domain.kassa.port.KassaPorts
import kz.mybrain.superkassa.presentation.common.model.WindowServices

/**
 * Модель продажи окна.
 *
 * Одна на окно: живёт в хранилище моделей окна, и корзина переживает
 * уход кассира в другой раздел. Проверки создают модель сами, без окна.
 */
@Composable
fun saleViewModel(services: WindowServices, ports: KassaPorts): SaleViewModel = viewModel { saleModel(services, ports) }

/** Модель со сценариями, собранными из портов окна; проверки зовут её без окна. */
fun saleModel(services: WindowServices, ports: KassaPorts): SaleViewModel =
    SaleViewModel(SaleCases(services.kassa, services.signIn, services.memory, ports), services.talk)
