package kz.mybrain.superkassa.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.section.AppTopBar
import kz.mybrain.superkassa.designsystem.strings.LocalStrings

/**
 * Настройки с экрана входа.
 *
 * Своего набора настроек здесь нет: экран настроек в приложении один,
 * и он сам отбирает, что показать — до входа кассы не выбрано и прав
 * администратора нет, поэтому остаются разделы того, что задают раньше,
 * чем куда-либо входят. Язык и тема — в разделе оформления, как и после
 * входа.
 *
 * Отличие только в обрамлении: сюда ведёт отдельная дверь, и из неё
 * нужен возврат. Шапка называет экран, и у списка разделов второго
 * заголовка нет.
 */
@Composable
fun WorkplaceSettingsScreen(board: SettingsBoard, onBack: () -> Unit) {
    val texts = LocalStrings.current
    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = texts.settingsScreen.workplace, onBack = onBack, backLabel = texts.general.hide) {}
        SettingsPanes(board, Modifier.weight(1f), titled = false)
    }
}
