package kz.mybrain.superkassa.presentation.print.target

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.mybrain.superkassa.domain.print.port.PrintPorts
import kz.mybrain.superkassa.presentation.common.model.WindowServices

/** Модель принтера кассы окна. */
@Composable
fun printTargetViewModel(services: WindowServices, ports: PrintPorts): PrintTargetViewModel =
    viewModel { printTargetModel(services, ports) }

/** Модель со сценариями, собранными из портов окна; проверки зовут её без окна. */
fun printTargetModel(services: WindowServices, ports: PrintPorts): PrintTargetViewModel =
    PrintTargetViewModel(PrintTargetCases(services.signIn, ports.printOut, ports.printChoices))
