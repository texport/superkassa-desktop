package kz.mybrain.superkassa.desktop.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.ui.components.AppTopBar
import kz.mybrain.superkassa.desktop.ui.components.ScrollableColumn
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Настройки рабочего места — те, что не требуют выбранной кассы.
 *
 * Вид, адреса узла и кабинета, службы карты. Они нужны до входа:
 * кассе, у которой не задан адрес узла, войти некуда, а кабинету
 * с чужим адресом — не в кого. Один набор карточек стоит и здесь,
 * и в общих настройках, чтобы не расходиться.
 */
@Composable
fun WorkplaceCards(session: Session) {
    AppearanceCard(session)
    NodeAddressCard(session)
    CabinetAddressCard(session)
    MapServicesCard(session)
}

/**
 * Экран настроек рабочего места с экрана входа.
 *
 * Полные настройки живут за входом — там кассы, печать и снятие
 * с учёта. Сюда ведёт отдельная дверь для того, что должно быть
 * задано раньше, чем куда-либо войти.
 */
@Composable
fun WorkplaceSettingsScreen(session: Session, onBack: () -> Unit) {
    val texts = LocalStrings.current
    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = texts.settings.workplace, onBack = onBack, backLabel = texts.common.hide) {}
        ScrollableColumn(
            modifier = Modifier.fillMaxSize().padding(Spacing.screen),
            spacing = Spacing.roomy
        ) {
            WorkplaceCards(session)
        }
    }
}
