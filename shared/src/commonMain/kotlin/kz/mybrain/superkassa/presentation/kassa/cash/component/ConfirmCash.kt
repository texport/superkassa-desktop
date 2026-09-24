package kz.mybrain.superkassa.presentation.kassa.cash.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kz.mybrain.superkassa.domain.kassa.model.cash.CashAttempt
import kz.mybrain.superkassa.domain.kassa.model.cash.CashHoldup
import kz.mybrain.superkassa.domain.kassa.model.cash.CashMove
import kz.mybrain.superkassa.domain.kassa.model.cash.CashRefusal
import kz.mybrain.superkassa.domain.kassa.model.cash.CashRules
import kz.mybrain.superkassa.presentation.common.format.Money
import kz.mybrain.superkassa.presentation.theme.icon.AppIcons
import kz.mybrain.superkassa.strings.api.fill
import kz.mybrain.superkassa.strings.api.kassa.DrawerTexts

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
        title = { Text(question.fill(Money.formatTiyn(pending.amount))) },
        text = {
            Text(
                text = after?.let { money.afterOperation.fill(Money.formatTiyn(it)) }
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

/**
 * Что сказано о сумме: как её вводить либо что мешает провести деньги.
 *
 * Разделено намеренно: правило ввода стоит подсказкой в самом поле и
 * уходит, как только кассир начал набирать, а помеха стоит строкой под
 * полем и видна, пока не исправлена. Прежде и то, и другое было одной
 * записью с признаком «ошибка», и помеха, не бывшая ошибкой ввода —
 * закрытая смена, — не показывалась вовсе.
 */
internal sealed interface CashAdvice {

    /** Как вводить сумму. */
    data class Hint(val text: String) : CashAdvice

    /**
     * Что мешает провести деньги.
     *
     * @param mistake ошибка в набранном — тогда поле красное.
     */
    data class Holdup(val text: String, val mistake: Boolean) : CashAdvice
}

/**
 * Что сказать под полем ввода: помеха словами кассира или подсказка ввода.
 *
 * Какая помеха названа и в каком порядке — решает правило ящика
 * [CashHoldup]; здесь только слова.
 */
internal fun adviceOn(holdup: CashHoldup?, money: DrawerTexts, drawer: Long?): CashAdvice {
    holdup ?: return CashAdvice.Hint(money.amountHint)
    val text = when (holdup.reason) {
        CashRefusal.NotANumber -> money.notANumber
        CashRefusal.NotPositive -> money.notPositive
        CashRefusal.TooLarge -> money.tooLarge
        CashRefusal.NotEnough -> money.notEnough.fill(Money.formatTiyn(drawer))
        CashRefusal.ShiftClosed -> money.shiftClosed
        CashRefusal.KkmBlocked -> money.kkmBlocked
    }
    return CashAdvice.Holdup(text, holdup.mistake)
}
