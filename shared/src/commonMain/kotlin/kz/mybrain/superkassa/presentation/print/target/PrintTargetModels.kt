package kz.mybrain.superkassa.presentation.print.target

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.mybrain.superkassa.presentation.shell.AppContainer

/** Модель принтера кассы окна. */
@Composable
fun printTargetViewModel(app: AppContainer): PrintTargetViewModel = viewModel { printTargetModel(app) }

/** Модель со сценариями, собранными из портов окна; проверки зовут её без окна. */
fun printTargetModel(app: AppContainer): PrintTargetViewModel {
    val ports = app.areas.settings
    return PrintTargetViewModel(PrintTargetCases(app.signIn, ports.printOut, ports.printChoices))
}
