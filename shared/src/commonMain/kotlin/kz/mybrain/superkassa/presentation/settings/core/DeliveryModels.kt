package kz.mybrain.superkassa.presentation.settings.core

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.mybrain.superkassa.domain.settings.port.SettingsPorts
import kz.mybrain.superkassa.presentation.common.model.WindowServices

/** Модель каналов доставки чека окна. */
@Composable
fun deliveryViewModel(services: WindowServices, ports: SettingsPorts): DeliveryViewModel =
    viewModel { deliveryModel(services, ports) }

/** Модель со сценариями из портов окна; проверки зовут её без окна. */
fun deliveryModel(services: WindowServices, ports: SettingsPorts): DeliveryViewModel =
    DeliveryViewModel(DeliveryCases(ports.coreSettings), services.talk)
