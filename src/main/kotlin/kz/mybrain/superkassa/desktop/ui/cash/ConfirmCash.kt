package kz.mybrain.superkassa.desktop.ui.cash

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kz.mybrain.superkassa.desktop.ui.components.Money
import kz.mybrain.superkassa.desktop.ui.strings.DrawerTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons

/** Вопрос перед проведением: сумма словами кассира и остаток после. */
@Composable
internal fun ConfirmCash(
    money: DrawerTexts,
    pending: CashAttempt,
    drawer: Long?,
    busy: Boolean,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    val question = if (pending.move == CashMove.Deposit) money.confirmDeposit else money.confirmWithdraw
    val after = CashRules.after(drawer, pending.amount, pending.move)
    AlertDialog(
        onDismissRequest = { if (!busy) onCancel() },
        icon = { Icon(AppIcons.drawer, contentDescription = null) },
        title = { Text(question.format(Money.format(pending.amount))) },
        text = {
            Text(
                text = after?.let { money.afterOperation.format(Money.formatTiyn(it)) }
                    ?: money.unknownBalance,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(enabled = !busy, onClick = onConfirm) {
                Text(if (busy) money.working else money.confirm)
            }
        },
        dismissButton = {
            TextButton(enabled = !busy, onClick = onCancel) { Text(money.cancel) }
        }
    )
}

/** Строка под полем суммы: что мешает провести деньги либо как их вводить. */
internal data class CashAdvice(val text: String, val error: Boolean)

/**
 * Что сказать под полем ввода.
 *
 * Строка под полем одна: три предупреждения столбиком читаются как три
 * разные беды. Закрытая смена и нехватка денег в ящике поле красным не
 * красят — введено верно, мешает состояние кассы, а не набранные цифры.
 */
internal fun adviceOn(
    deposit: CashDecision,
    withdraw: CashDecision,
    money: DrawerTexts,
    drawer: Long?
): CashAdvice {
    val shortage = (withdraw as? CashDecision.Refused)?.reason == CashRefusal.NotEnough
    val refused = deposit as? CashDecision.Refused
        ?: return when {
            shortage -> CashAdvice(money.notEnough.format(Money.formatTiyn(drawer)), error = false)
            else -> CashAdvice(money.amountHint, error = false)
        }
    return when (refused.reason) {
        CashRefusal.NotANumber -> CashAdvice(money.notANumber, error = true)
        CashRefusal.NotPositive -> CashAdvice(money.notPositive, error = true)
        CashRefusal.TooLarge -> CashAdvice(money.tooLarge, error = true)
        CashRefusal.NotEnough -> CashAdvice(money.notEnough.format(Money.formatTiyn(drawer)), error = true)
        CashRefusal.ShiftClosed -> CashAdvice(money.shiftClosed, error = false)
        // Блокировка — состояние кассы, а не ошибка ввода: снятой
        // с учёта кассе узел движений наличных не проводит.
        CashRefusal.KkmBlocked -> CashAdvice(money.kkmBlocked, error = false)
    }
}
