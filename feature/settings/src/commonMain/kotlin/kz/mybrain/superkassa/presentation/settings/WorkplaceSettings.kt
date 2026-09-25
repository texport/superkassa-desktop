package kz.mybrain.superkassa.presentation.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.navigation.step.SettingsSectionKey

/**
 * Настройки с экрана входа.
 *
 * Своего набора настроек здесь нет: экран настроек в приложении один,
 * и он сам отбирает, что показать — до входа кассы не выбрано и прав
 * администратора нет, поэтому остаются разделы приложения и кабинета.
 *
 * Шапки у экрана нет: её вместе с возвратом на вход ставит каркас окна
 * в свой единственный слот, и она же называет экран.
 *
 * @param opened раздел, открытый поверх списка историей окна.
 */
@Composable
fun WorkplaceSettingsScreen(board: SettingsBoard, opened: SettingsSectionKey? = null) {
    SettingsPanes(board, opened, Modifier.fillMaxSize())
}
