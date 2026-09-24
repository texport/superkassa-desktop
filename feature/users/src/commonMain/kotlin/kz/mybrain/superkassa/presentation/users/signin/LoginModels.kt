package kz.mybrain.superkassa.presentation.users.signin

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.mybrain.superkassa.presentation.common.model.WindowServices

/**
 * Модель входа окна.
 *
 * Одна на окно: набранное кассиром и прочитанный список касс переживают
 * переход за дверь — в кабинет или настройки — и обратно. Ею же кассир
 * уходит из рабочего окна: вход и уход — один сценарий области.
 */
@Composable
fun loginViewModel(services: WindowServices): LoginViewModel = viewModel { loginModel(services) }

/** Модель со сценариями, собранными из портов окна; проверки зовут её без окна. */
fun loginModel(services: WindowServices): LoginViewModel =
    LoginViewModel(LoginCases(services.kassa, services.signIn, services.memory, services.talk.journal), services.talk)
