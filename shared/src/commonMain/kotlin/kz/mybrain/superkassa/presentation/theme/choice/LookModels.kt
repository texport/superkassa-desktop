package kz.mybrain.superkassa.presentation.theme.choice

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.mybrain.superkassa.presentation.shell.AppContainer

/**
 * Модель вида окна.
 *
 * Одна на окно: живёт в хранилище моделей окна, и тема, шапка, настройки
 * и кабинет берут один и тот же экземпляр.
 */
@Composable
fun lookViewModel(app: AppContainer): LookViewModel = viewModel { lookModel(app) }

/** Модель над видом рабочего места; проверки зовут её без окна. */
fun lookModel(app: AppContainer): LookViewModel = LookViewModel(LookCases(app.look))
