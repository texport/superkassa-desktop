package kz.mybrain.superkassa.presentation.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.navigation.settings.SettingsSectionKey

/**
 * Настройки — один экран на всё приложение.
 *
 * Открывается из двух мест: с экрана входа, где кассы ещё нет, и из кассы,
 * куда кассир вошёл. Прежде это были два разных экрана со своими списками
 * карточек, и они разошлись: отладка стояла только за входом — то есть
 * ровно там, где она уже не нужна, потому что войти получилось.
 *
 * Список настроек один, а показывается каждая по своим условиям:
 * настройке кассы нужна выбранная касса, служебной — права
 * администратора. До входа ни того, ни другого нет, и остаются разделы
 * того, что задают раньше, чем куда-либо войти.
 *
 * Разложены настройки «списком и подробностями» — см. [SettingsPanes].
 *
 * @param opened раздел, открытый поверх списка историей окна; `null` —
 *   открыт список (на широком окне — с разделом рядом).
 */
@Composable
fun SettingsScreen(board: SettingsBoard, opened: SettingsSectionKey? = null) {
    SettingsPanes(board, opened, Modifier.fillMaxSize())
}
