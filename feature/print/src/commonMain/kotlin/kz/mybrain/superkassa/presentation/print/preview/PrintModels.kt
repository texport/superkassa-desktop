package kz.mybrain.superkassa.presentation.print.preview

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.mybrain.superkassa.domain.print.port.PrintPorts
import kz.mybrain.superkassa.presentation.common.model.WindowServices

/** Модель печати окна: одна на все разделы и на дверь в кабинет. */
@Composable
internal fun printViewModel(services: WindowServices, ports: PrintPorts): PrintViewModel =
    viewModel { printModel(services, ports) }

/** Модель со сценариями, собранными из портов окна; проверки зовут её без окна. */
internal fun printModel(services: WindowServices, ports: PrintPorts): PrintViewModel = PrintViewModel(
    PrintCases(services.kassa, services.signIn, ports.printOut, ports.printChoices, services.memory),
    services.talk
)
