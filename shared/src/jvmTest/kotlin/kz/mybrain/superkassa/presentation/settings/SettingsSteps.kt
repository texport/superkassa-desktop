package kz.mybrain.superkassa.presentation.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavKey
import kz.mybrain.superkassa.navigation.settings.SettingsSectionKey
import kz.mybrain.superkassa.presentation.shell.frame.StepsOf

/**
 * Настройки с историей шагов, как в окне: на узком окне раздел,
 * нажатый в списке, открывается поверх него шагом истории.
 *
 * @param content настройки: открытый поверх списка раздел и шаг назад —
 *   `null`, пока открыт сам список.
 */
@Composable
internal fun SettingsSteps(content: @Composable (opened: SettingsSectionKey?, back: (() -> Unit)?) -> Unit) {
    val steps = remember { mutableStateListOf<NavKey>() }
    val back = { steps.removeLastOrNull().let { } }
    StepsOf(steps, back) { content(steps.lastOrNull() as? SettingsSectionKey, back.takeIf { steps.isNotEmpty() }) }
}
