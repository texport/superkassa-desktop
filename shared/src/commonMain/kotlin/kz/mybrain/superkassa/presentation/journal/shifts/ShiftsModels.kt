package kz.mybrain.superkassa.presentation.journal.shifts

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.mybrain.superkassa.presentation.common.model.WindowServices

/**
 * Модель прошлых смен окна.
 *
 * Одна на окно: открытая смена и прочитанные страницы переживают уход
 * кассира в продажу и обратно.
 */
@Composable
fun shiftsViewModel(services: WindowServices): ShiftsViewModel = viewModel { shiftsModel(services) }

/** Модель со сценариями, собранными из портов окна; проверки зовут её без окна. */
fun shiftsModel(services: WindowServices): ShiftsViewModel =
    ShiftsViewModel(ShiftsCases(services.kassa, services.signIn), services.talk)
