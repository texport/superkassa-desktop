package kz.mybrain.superkassa.presentation.print.preview

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.mybrain.superkassa.presentation.shell.AppContainer

/** Модель печати окна: одна на все разделы и на дверь в кабинет. */
@Composable
fun printViewModel(app: AppContainer): PrintViewModel = viewModel { printModel(app) }

/** Модель со сценариями, собранными из портов окна; проверки зовут её без окна. */
fun printModel(app: AppContainer): PrintViewModel {
    val ports = app.areas.settings
    return PrintViewModel(PrintCases(app.kassa, app.signIn, ports.printOut, ports.printChoices, app.memory), app.talk)
}
