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
     * @param mistake ошибка в набранном — тогда поле красное. Состояние
     * кассы поле красным не красит: введено верно.
     */
    data class Holdup(val text: String, val mistake: Boolean) : CashAdvice
}

/**
 * Что сказать под полем ввода.
 *
 * Строка под полем одна: три предупреждения столбиком читаются как три
 * разные беды. Закрытая смена и нехватка денег в ящике поле красным не
 * красят — введено верно, мешает состояние кассы, а не набранные цифры.
 *
 * Состояние кассы названо раньше набранного и независимо от него: при
 * пустом поле правило отвечает «ещё ничего не введено», и экран закрытой
 * смены выходил неотличимым от обычного — две погашенные кнопки и ни
 * слова о том, почему они погасли.
 *
 * @param shiftOpen открыта ли смена: закрытой узел движений не проводит.
 * @param kkmBlocked заблокирована ли касса — в том числе снята с учёта.
 */
internal fun adviceOn(
    deposit: CashDecision,
    withdraw: CashDecision,
    money: DrawerTexts,
    drawer: Long?,
    shiftOpen: Boolean = true,
    kkmBlocked: Boolean = false
): CashAdvice {
    // Блокировка и закрытая смена — состояние кассы, а не ошибка ввода:
    // снятой с учёта кассе узел движений наличных не проводит.
    if (kkmBlocked) return CashAdvice.Holdup(money.kkmBlocked, mistake = false)
    if (!shiftOpen) return CashAdvice.Holdup(money.shiftClosed, mistake = false)
    val shortage = (withdraw as? CashDecision.Refused)?.reason == CashRefusal.NotEnough
    val refused = deposit as? CashDecision.Refused
        ?: return when {
            shortage -> CashAdvice.Holdup(money.notEnough.format(Money.formatTiyn(drawer)), mistake = false)
            else -> CashAdvice.Hint(money.amountHint)
        }
    return when (refused.reason) {
        CashRefusal.NotANumber -> CashAdvice.Holdup(money.notANumber, mistake = true)
        CashRefusal.NotPositive -> CashAdvice.Holdup(money.notPositive, mistake = true)
        CashRefusal.TooLarge -> CashAdvice.Holdup(money.tooLarge, mistake = true)
        CashRefusal.NotEnough ->
            CashAdvice.Holdup(money.notEnough.format(Money.formatTiyn(drawer)), mistake = true)

        CashRefusal.ShiftClosed -> CashAdvice.Holdup(money.shiftClosed, mistake = false)
        CashRefusal.KkmBlocked -> CashAdvice.Holdup(money.kkmBlocked, mistake = false)
    }
}
