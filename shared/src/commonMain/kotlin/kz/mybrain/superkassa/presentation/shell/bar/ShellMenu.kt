package kz.mybrain.superkassa.presentation.shell.bar

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kz.mybrain.superkassa.designsystem.adaptive.LocalWindowClass
import kz.mybrain.superkassa.designsystem.adaptive.WidthClass
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.presentation.common.look.LookViewModel
import kz.mybrain.superkassa.presentation.common.picker.LanguageMenuItems
import kz.mybrain.superkassa.presentation.common.picker.LanguagePicker
import kz.mybrain.superkassa.presentation.common.picker.ThemeMenuItem
import kz.mybrain.superkassa.presentation.common.picker.ThemeSwitch
import kz.mybrain.superkassa.presentation.common.status.KkmStatusChips
import kz.mybrain.superkassa.presentation.shell.frame.ShellUiState

/**
 * Действия шапки кассы по ширине окна.
 *
 * В шапке первыми сжимались название кассы и организация, а имя кассира
 * пропадало совсем: действия справа меряются раньше заголовка и берут
 * сколько им нужно. По Material 3 то, чему не хватает места в строке
 * заголовка, уходит в меню «Ещё», — так и здесь:
 *
 * - большое окно и шире — всё значками, как было;
 * - расширенное — обновить, тема и язык уходят в меню;
 * - среднее и уже — туда же уходит «Сменить кассира».
 *
 * Плашки состояния остаются на виду при любой ширине: о блокировке
 * и разрыве связи кассир узнаёт до того, как пробьёт чек.
 */
@Composable
internal fun KkmBarActions(shell: ShellUiState, look: LookViewModel, onSignOut: () -> Unit, onRefresh: () -> Unit) {
    val texts = LocalStrings.current
    val width = LocalWindowClass.current.width
    KkmStatusChips(shell.kkm)
    if (width >= WidthClass.Large) {
        IconButton(onClick = onRefresh) {
            Icon(AppIcons.refresh, contentDescription = texts.common.refresh)
        }
        ThemeSwitch(look)
        LanguagePicker(look)
    }
    val signOutFolded = width < WidthClass.Expanded
    if (width < WidthClass.Large) {
        ShellMenu(look, onRefresh, onSignOut.takeIf { signOutFolded })
    }
    if (!signOutFolded) {
        TextButton(onClick = onSignOut) { Text(texts.shell.changeCashier) }
    }
}

/**
 * Меню «Ещё»: те же действия, что значками в широком окне.
 *
 * @param onSignOut смена кассира, если и ей не хватило места в строке.
 */
@Composable
private fun ShellMenu(look: LookViewModel, onRefresh: () -> Unit, onSignOut: (() -> Unit)?) {
    val texts = LocalStrings.current
    var open by remember { mutableStateOf(false) }
    val close = { open = false }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(AppIcons.moreActions, contentDescription = texts.shell.moreActions)
        }
        DropdownMenu(expanded = open, onDismissRequest = close) {
            ShellMenuItems(look, close, onRefresh, onSignOut)
        }
    }
}

/** Пункты меню «Ещё»: обновить, тема, языки и, если ушла сюда, смена кассира. */
@Composable
private fun ShellMenuItems(look: LookViewModel, close: () -> Unit, onRefresh: () -> Unit, onSignOut: (() -> Unit)?) {
    val texts = LocalStrings.current
    DropdownMenuItem(
        text = { Text(texts.common.refresh) },
        leadingIcon = { Icon(AppIcons.refresh, contentDescription = null) },
        onClick = {
            close()
            onRefresh()
        }
    )
    ThemeMenuItem(look, close)
    HorizontalDivider()
    LanguageMenuItems(look, close)
    if (onSignOut != null) {
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text(texts.shell.changeCashier) },
            onClick = {
                close()
                onSignOut()
            }
        )
    }
}
