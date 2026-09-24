package kz.mybrain.superkassa.presentation.users

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.mybrain.superkassa.presentation.common.model.WindowServices

/**
 * Модель кассиров окна.
 *
 * Одна на окно: набранное в заведении кассира переживает уход в другой
 * раздел. Проверки создают модель сами, без окна.
 */
@Composable
fun usersViewModel(services: WindowServices): UsersViewModel = viewModel { usersModel(services) }

/** Модель со сценариями, собранными из портов окна; проверки зовут её без окна. */
internal fun usersModel(services: WindowServices): UsersViewModel =
    UsersViewModel(UsersCases(services.kassa, services.signIn), services.talk)
