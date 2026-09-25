package kz.mybrain.superkassa.presentation.shell.frame

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.section.barLeadStart
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons

/**
 * Все разделы с названиями — модальной панелью навигации Material 3
 * поверх окна: на телефоне и на планшете стоймя, когда разделов больше
 * пяти, и по кнопке меню у рельса.
 *
 * Панель — продолжение того, что было на месте: её верхний ряд повторяет
 * ряд с кнопкой меню, и кнопка закрытия встаёт ровно туда, где была кнопка
 * меню, — у рельса на его оси ([MenuRow]), на телефоне — в начале строки
 * шапки ([barLeadStart]). Значки разделов стоят на той же оси, что
 * в рельсе: поля пункта панели Material 3 ставят центр значка на середину
 * рельса. Прежде разделы выезжали модальным широким рельсом, и его
 * собственная шапка ставила кнопку закрытия ниже и в другом месте.
 *
 * @param fromRail панель открыта кнопкой у рельса, а не в шапке.
 * @param enabled у окна есть кнопка меню; без неё панели нет вовсе.
 */
@Composable
internal fun SectionDrawer(
    state: DrawerState,
    fromRail: Boolean,
    enabled: Boolean,
    onClose: () -> Unit,
    items: SectionItems<*>,
    content: @Composable () -> Unit
) {
    if (!enabled) return content()
    ModalNavigationDrawer(
        drawerState = state,
        gesturesEnabled = state.isOpen,
        drawerContent = {
            ModalDrawerSheet(windowInsets = RailInsets) {
                DrawerHead(fromRail, onClose)
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) { items.drawer() }
            }
        },
        content = content
    )
}

/** Верхний ряд панели: кнопка закрытия на месте кнопки меню. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DrawerHead(fromRail: Boolean, onClose: () -> Unit) {
    val close: @Composable () -> Unit = {
        IconButton(onClick = onClose) {
            Icon(AppIcons.menuOpen, contentDescription = LocalStrings.current.general.collapse)
        }
    }
    if (fromRail) {
        MenuRow(close)
    } else {
        Box(
            modifier = Modifier.height(TopAppBarDefaults.TopAppBarExpandedHeight).padding(start = barLeadStart),
            contentAlignment = Alignment.CenterStart
        ) { close() }
    }
}
