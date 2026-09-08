package kz.mybrain.superkassa.desktop.ui.users

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import kz.mybrain.superkassa.desktop.ui.components.InfoTip
import kz.mybrain.superkassa.desktop.ui.components.RecordRow
import kz.mybrain.superkassa.desktop.ui.history.DASH
import kz.mybrain.superkassa.desktop.ui.strings.CashierTexts
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.MoneyTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
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
    deletable: Boolean,
    onChangePin: suspend (String) -> Boolean,
    onRemove: () -> Unit
) {
    val texts = LocalStrings.current
    val cashiers = money.cashiers
    var pinAsked by remember { mutableStateOf(false) }
    var deleteAsked by remember { mutableStateOf(false) }

    RecordRow(
        title = user.name ?: DASH,
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
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = texts.users.delete)
                }
            }
        }
    )

    if (pinAsked) {
        ChangePinDialog(
            money = money,
            who = user.name ?: roleTitle,
            onDismiss = { pinAsked = false },
            onConfirm = onChangePin
        )
    }
    if (deleteAsked) {
        ConfirmDelete(
            money = cashiers,
            who = user.name ?: roleTitle,
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

/** Вопрос перед удалением: кого и что при этом останется. */
@Composable
private fun ConfirmDelete(
    money: CashierTexts,
    who: String,
    cancel: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    val texts = LocalStrings.current
    AlertDialog(
        onDismissRequest = onCancel,
        icon = {
            Icon(
                imageVector = Icons.Outlined.WarningAmber,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text(money.deleteConfirm.format(who)) },
        text = { Text(money.deleteExplain, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) { Text(texts.users.delete) }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text(cancel) } }
    )
}
