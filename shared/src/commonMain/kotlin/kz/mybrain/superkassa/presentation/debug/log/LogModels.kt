package kz.mybrain.superkassa.presentation.debug.log

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.mybrain.superkassa.domain.debug.port.DebugPorts
import kz.mybrain.superkassa.presentation.common.model.WindowServices

/** Модель журнала окна: одна на окно кассы и на соседнее окно отладки. */
@Composable
fun logViewModel(services: WindowServices, ports: DebugPorts): LogViewModel = viewModel { logModel(services, ports) }

/** Модель со сценариями, собранными из портов окна; проверки зовут её без окна. */
fun logModel(services: WindowServices, ports: DebugPorts): LogViewModel =
    LogViewModel(LogCases(ports.logBook), services.talk)
