package kz.mybrain.superkassa.presentation.journal.shifts

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.mybrain.superkassa.presentation.shell.AppContainer

/**
 * Модель прошлых смен окна.
 *
 * Одна на окно: открытая смена и прочитанные страницы переживают уход
 * кассира в продажу и обратно.
 */
@Composable
fun shiftsViewModel(app: AppContainer): ShiftsViewModel = viewModel { shiftsModel(app) }

/** Модель со сценариями, собранными из портов окна; проверки зовут её без окна. */
fun shiftsModel(app: AppContainer): ShiftsViewModel = ShiftsViewModel(ShiftsCases(app.kassa, app.signIn), app.talk)
