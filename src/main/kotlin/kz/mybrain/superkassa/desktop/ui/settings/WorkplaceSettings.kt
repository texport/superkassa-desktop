package kz.mybrain.superkassa.desktop.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.ui.components.AppTopBar
import kz.mybrain.superkassa.desktop.ui.components.LanguagePicker
import kz.mybrain.superkassa.desktop.ui.components.ScrollableColumn
import kz.mybrain.superkassa.desktop.ui.components.ThemeSwitch
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Настройки с экрана входа.
 *
 * Своего набора карточек здесь нет: экран настроек в приложении один,
 * и он сам отбирает, что показать — до входа кассы не выбрано и прав
 * администратора нет, поэтому остаётся то, что задают раньше, чем
 * куда-либо входят. Прежде здесь стоял второй список карточек, и он
 * разошёлся с первым.
 *
 * Отличие только в обрамлении: сюда ведёт отдельная дверь, и из неё
 * нужен возврат.
 */
@Composable
fun WorkplaceSettingsScreen(session: Session, onBack: () -> Unit) {
    val texts = LocalStrings.current
    Column(modifier = Modifier.fillMaxSize()) {
        // Оформление и язык переключаются и здесь: на экране входа они есть,
        // и терять их, зайдя в настройки, странно — особенно язык, которым
        // читают сами настройки.
        AppTopBar(title = texts.settings.workplace, onBack = onBack, backLabel = texts.common.hide) {
            ThemeSwitch(session)
            LanguagePicker(session)
        }
        ScrollableColumn(
            modifier = Modifier.fillMaxSize().padding(Spacing.screen),
            spacing = Spacing.roomy
        ) {
            SettingsCards(session)
        }
    }
}
