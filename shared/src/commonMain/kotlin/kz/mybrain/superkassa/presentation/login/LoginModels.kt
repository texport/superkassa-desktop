package kz.mybrain.superkassa.presentation.login

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.mybrain.superkassa.presentation.shell.AppContainer

/**
 * Модель входа окна.
 *
 * Одна на окно: набранное кассиром и прочитанный список касс переживают
 * переход за дверь — в кабинет или настройки — и обратно. Ею же кассир
 * уходит из рабочего окна: вход и уход — один сценарий области.
 */
@Composable
fun loginViewModel(app: AppContainer): LoginViewModel = viewModel { loginModel(app) }

/** Модель со сценариями, собранными из портов окна; проверки зовут её без окна. */
fun loginModel(app: AppContainer): LoginViewModel =
    LoginViewModel(LoginCases(app.kassa, app.signIn, app.memory, app.journal), app.talk)
