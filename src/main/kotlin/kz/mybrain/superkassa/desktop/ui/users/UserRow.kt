package kz.mybrain.superkassa.desktop.ui.users

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import kz.mybrain.superkassa.desktop.server.KkmUser
import kz.mybrain.superkassa.desktop.ui.components.Chip
import kz.mybrain.superkassa.desktop.ui.components.ConfirmDangerDialog
import kz.mybrain.superkassa.desktop.ui.components.InfoTip
import kz.mybrain.superkassa.desktop.ui.components.RecordRow
import kz.mybrain.superkassa.desktop.ui.strings.CashierTexts
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.MoneyTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Строка кассира: имя, роль и действия над ним.
 *
 * Смена пина уехала в диалог: поле пина в каждой строке списка делало
 * из перечня кассиров форму на десять полей, а пин меняют раз в полгода.
 *
 * Удаление последнего носителя роли не просто отклоняется узлом, а гасится
 * здесь с объяснением: отказ «Нужен хотя бы один CASHIER» приходит кодом
 * роли, и администратор читает его как поломку, а не как правило.
 */
@Composable
internal fun UserRow(
    money: MoneyTexts,
    roleTitle: String,
    user: KkmUser,
    /** Это тот кассир, который сейчас работает: только ему меняют пин себе. */
    own: Boolean,
    deletable: Boolean,
    onChangePin: suspend (String) -> Boolean,
    onRemove: () -> Unit
) {
    val texts = LocalStrings.current
    val cashiers = money.cashiers
    var pinAsked by remember { mutableStateOf(false) }
    var deleteAsked by remember { mutableStateOf(false) }

    RecordRow(
        title = user.name ?: Glyphs.DASH,
        leading = {
            Icon(
                imageVector = AppIcons.cashiers,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        support = { WhoIs(cashiers, roleTitle, deletable) },
        trailing = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { pinAsked = true }) { Text(texts.users.newPin) }
                IconButton(
                    enabled = deletable,
                    onClick = { deleteAsked = true },
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(AppIcons.remove, contentDescription = texts.users.delete)
                }
            }
        }
    )

    if (pinAsked) {
        ChangePinDialog(
            money = money,
            who = user.name ?: roleTitle,
            own = own,
            onDismiss = { pinAsked = false },
            onConfirm = onChangePin
        )
    }
    if (deleteAsked) {
        ConfirmDangerDialog(
            what = cashiers.deleteConfirm.format(user.name ?: roleTitle),
            explain = cashiers.deleteExplain,
            action = LocalStrings.current.users.delete,
            cancel = money.drawer.cancel,
            onCancel = { deleteAsked = false },
            onConfirm = {
                deleteAsked = false
                onRemove()
            }
        )
    }
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
                InfoTip(money.deleteBlocked.format(roleTitle))
            }
        }
    }
}
