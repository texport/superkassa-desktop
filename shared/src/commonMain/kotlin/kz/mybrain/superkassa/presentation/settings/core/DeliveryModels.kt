package kz.mybrain.superkassa.presentation.settings.core

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.mybrain.superkassa.presentation.shell.AppContainer

/** Модель каналов доставки чека окна. */
@Composable
fun deliveryViewModel(app: AppContainer): DeliveryViewModel = viewModel { deliveryModel(app) }

/** Модель со сценариями из портов окна; проверки зовут её без окна. */
fun deliveryModel(app: AppContainer): DeliveryViewModel =
    DeliveryViewModel(DeliveryCases(app.areas.settings.coreSettings), app.talk)
