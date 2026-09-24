package kz.mybrain.superkassa.presentation.users

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import io.github.texport.superkassa.core.presentation.api.model.user.UserResponse
import kz.mybrain.superkassa.presentation.common.dialog.ConfirmDangerDialog
import kz.mybrain.superkassa.presentation.common.format.fill
import kz.mybrain.superkassa.presentation.common.list.RecordRow
import kz.mybrain.superkassa.presentation.common.message.InfoTip
import kz.mybrain.superkassa.presentation.common.status.Chip
import kz.mybrain.superkassa.presentation.strings.common.LocalStrings
import kz.mybrain.superkassa.presentation.strings.kassa.CashierTexts
import kz.mybrain.superkassa.presentation.strings.kassa.MoneyTexts
import kz.mybrain.superkassa.presentation.theme.icon.AppIcons
import kz.mybrain.superkassa.presentation.theme.icon.Glyphs
import kz.mybrain.superkassa.presentation.theme.size.Spacing

/**
 * Строка кассира: имя, роль и действия над ним.
 *
 * Смена пина уехала в диалог: поле пина в каждой строке списка делало
 * из перечня кассиров форму на десять полей, а пин меняют раз в полгода.
 *
 * Удаление единственного администратора гасится здесь с объяснением:
 * отказ кассы приходит кодом роли, и администратор читает его как поломку,
 * а не как правило. Остальных, включая единственного кассира, удалять
 * можно: касса без кассиров — обычное её состояние после подключения.
 */
@Composable
internal fun UserRow(
    money: MoneyTexts,
    roleTitle: String,
    user: UserResponse,
    deletable: Boolean,
    actions: UsersActions
) {
    RecordRow(
        title = user.name.ifBlank { Glyphs.DASH },
        leading = {
            Icon(
                imageVector = AppIcons.cashiers,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        support = { WhoIs(money.cashiers, roleTitle, deletable) },
        trailing = { RowActions(user, deletable, actions) }
    )
}

/** Новый пин и корзина; корзина единственного администратора погашена. */
@Composable
private fun RowActions(user: UserResponse, deletable: Boolean, actions: UsersActions) {
    val texts = LocalStrings.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = { actions.askPin(user) }) { Text(texts.users.newPin) }
        IconButton(
            enabled = deletable,
            onClick = { actions.askRemove(user) },
            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(AppIcons.remove, contentDescription = texts.users.delete)
        }
    }
}

/** Вопрос перед удалением: кассир вместе с пином уходит с кассы насовсем. */
@Composable
internal fun RemoveDialog(money: MoneyTexts, user: UserResponse, actions: UsersActions) {
    ConfirmDangerDialog(
        what = money.cashiers.deleteConfirm.fill(user.name),
        explain = money.cashiers.deleteExplain,
        action = LocalStrings.current.users.delete,
        cancel = money.drawer.cancel,
        onCancel = { actions.askRemove(null) },
        onConfirm = actions::confirmRemove
    )
}

/** Роль кассира и, если удалить его нельзя, — почему. */
@Composable
private fun WhoIs(money: CashierTexts, roleTitle: String, deletable: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.hairline)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(roleTitle)
            if (!deletable) {
                Chip(money.onlyInRole, MaterialTheme.colorScheme.outline)
                // Почему удалить нельзя — под значком: строка на каждой
                // строке списка удваивает высоту перечня кассиров.
                InfoTip(money.deleteBlocked)
            }
        }
    }
}
