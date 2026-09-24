package kz.mybrain.superkassa.presentation.users

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.mybrain.superkassa.presentation.shell.AppContainer

/**
 * Модель кассиров окна.
 *
 * Одна на окно: набранное в заведении кассира переживает уход в другой
 * раздел. Проверки создают модель сами, без окна.
 */
@Composable
fun usersViewModel(app: AppContainer): UsersViewModel = viewModel { usersModel(app) }

/** Модель со сценариями, собранными из портов окна; проверки зовут её без окна. */
fun usersModel(app: AppContainer): UsersViewModel = UsersViewModel(UsersCases(app.kassa, app.signIn), app.talk)
