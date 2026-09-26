package kz.mybrain.superkassa.presentation.shell.bar

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kz.mybrain.superkassa.designsystem.adaptive.LocalWindowClass
import kz.mybrain.superkassa.designsystem.adaptive.WidthClass
import kz.mybrain.superkassa.designsystem.section.BarAction
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
 * - уже — обновить, тема и язык уходят в меню.
 *
 * «Сменить кассира» — значком с подписью в подсказке при любой ширине:
 * надпись занимала полстроки, а значок места почти не берёт. Меню «Ещё»
 * по Material 3 стоит последним в строке.
 *
 * Плашки состояния остаются на виду при любой ширине: о блокировке
 * и разрыве связи кассир узнаёт до того, как пробьёт чек.
 */
@Composable
internal fun KkmBarActions(shell: ShellUiState, look: LookViewModel, onSignOut: () -> Unit, onRefresh: () -> Unit) {
    val texts = LocalStrings.current
    val width = LocalWindowClass.current.width
    KkmStatusChips(shell.kkm, all = width > WidthClass.Compact)
    if (width >= WidthClass.Large) {
        IconButton(onClick = onRefresh) {
            Icon(AppIcons.refresh, contentDescription = texts.general.refresh)
        }
        ThemeSwitch(look)
        LanguagePicker(look)
    }
    BarAction(AppIcons.changeCashier, texts.topBar.changeCashier, onSignOut)
    if (width < WidthClass.Large) {
        ShellMenu(look, onRefresh)
    }
}

/** Меню «Ещё»: те же действия, что значками в широком окне. */
@Composable
private fun ShellMenu(look: LookViewModel, onRefresh: () -> Unit) {
    val texts = LocalStrings.current
    var open by remember { mutableStateOf(false) }
    val close = { open = false }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(AppIcons.moreActions, contentDescription = texts.topBar.moreActions)
        }
        DropdownMenu(expanded = open, onDismissRequest = close) {
            ShellMenuItems(look, close, onRefresh)
        }
    }
}

/** Пункты меню «Ещё»: обновить, тема и языки. */
@Composable
private fun ShellMenuItems(look: LookViewModel, close: () -> Unit, onRefresh: () -> Unit) {
    val texts = LocalStrings.current
    DropdownMenuItem(
        text = { Text(texts.general.refresh) },
        leadingIcon = { Icon(AppIcons.refresh, contentDescription = null) },
        onClick = {
            close()
            onRefresh()
        }
    )
    ThemeMenuItem(look, close)
    HorizontalDivider()
    LanguageMenuItems(look, close)
}
