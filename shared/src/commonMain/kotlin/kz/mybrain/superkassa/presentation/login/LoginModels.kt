package kz.mybrain.superkassa.presentation.login

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.mybrain.superkassa.presentation.AppContainer

/**
 * Модель входа окна.
 *
 * Одна на окно: набранное кассиром и прочитанный список касс переживают
 * переход за дверь — в кабинет или настройки — и обратно.
 */
@Composable
fun loginViewModel(app: AppContainer): LoginViewModel = viewModel { LoginViewModel(app) }
