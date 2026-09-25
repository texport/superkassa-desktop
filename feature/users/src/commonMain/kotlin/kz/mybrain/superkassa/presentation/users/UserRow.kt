package kz.mybrain.superkassa.presentation.users

import androidx.compose.foundation.layout.Arrangement
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
import kz.mybrain.superkassa.designsystem.dialog.ConfirmDangerDialog
import kz.mybrain.superkassa.designsystem.list.RecordRow
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.designsystem.theme.icon.Glyphs
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.designsystem.tip.WarningTip
import kz.mybrain.superkassa.strings.api.fill
import kz.mybrain.superkassa.strings.api.kassa.CashierTexts
import kz.mybrain.superkassa.strings.api.kassa.MoneyTexts

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
        support = { Text(roleTitle) },
        trailing = { RowActions(money.cashiers, user, deletable, actions) }
    )
}

/**
 * Новый пин и корзина. У единственного администратора на месте корзины —
 * предупреждение: удалить его нельзя, и прямо там, где удаляют, значок
 * объясняет почему. Погашенная корзина молчала, а плашка с надписью
 * у роли делала его строку выше соседних.
 */
@Composable
private fun RowActions(money: CashierTexts, user: UserResponse, deletable: Boolean, actions: UsersActions) {
    val texts = LocalStrings.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.buttonGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = { actions.askPin(user) }) { Text(texts.users.newPin) }
        if (!deletable) return@Row WarningTip(money.onlyInRole, money.deleteBlocked)
        IconButton(
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
