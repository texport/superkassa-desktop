package kz.mybrain.superkassa.desktop.ui.cash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.ui.components.FieldButton
import kz.mybrain.superkassa.desktop.ui.components.FieldButtonKind
import kz.mybrain.superkassa.desktop.ui.components.fieldWidth
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.MoneyTexts
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import java.math.BigDecimal
import java.util.UUID

/**
 * Ввод суммы, подтверждение и проведение.
 *
 * Подтверждение стоит между вводом и фискальным документом намеренно:
 * изъятие тысячи и изъятие десяти тысяч отличаются одним нажатием,
 * а отменить проведённое движение денег нельзя.
 *
 * Внесение — заполненная кнопка, изъятие — тональная: на экране одно
 * главное действие, и деньги в кассу кладут чаще, чем достают.
 */
@Composable
internal fun CashForm(
    session: Session,
    money: MoneyTexts,
    onSubmit: suspend (CashMove, BigDecimal, String) -> Boolean
) {
    val texts = LocalStrings.current
    val scope = rememberCoroutineScope()
    var amount by remember { mutableStateOf("") }
    var attempt by remember { mutableStateOf<CashAttempt?>(null) }
    var asked by remember { mutableStateOf<CashAttempt?>(null) }
    var busy by remember { mutableStateOf(false) }

    val drawer = session.cashInDrawer
    val blocked = session.selected?.isBlocked == true
    val deposit = CashRules.check(amount, CashMove.Deposit, drawer, session.shiftOpen, blocked)
    val withdraw = CashRules.check(amount, CashMove.Withdraw, drawer, session.shiftOpen, blocked)
    val ready = session.selected != null && !busy
    val advice = adviceOn(deposit, withdraw, money.drawer, drawer)

    fun ask(move: CashMove, decision: CashDecision) {
        val value = (decision as? CashDecision.Ready)?.amount ?: return
        // Повторная попытка той же суммы идёт с прежним ключом: узел не
        // должен провести одни и те же деньги дважды.
        val next = CashRules.attemptFor(attempt, move, value) { "desktop-cash-${UUID.randomUUID()}" }
        attempt = next
        asked = next
    }

    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.roomy),
            verticalArrangement = Arrangement.spacedBy(Spacing.snug)
        ) {
            // Сумма и оба действия стоят одной строкой и одного роста:
            // кнопка под полем читается как отдельный блок, хотя это одно
            // действие — «внести столько-то».
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
                verticalAlignment = Alignment.Top
            ) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text(texts.common.amount) },
                    singleLine = true,
                    isError = advice.error,
                    // Совет под полем — только когда он о помехе: строка «сумма
                    // в тенге» под каждым внесением занимает место постоянно.
                    supportingText = if (advice.error) {
                        { Text(advice.text) }
                    } else {
                        null
                    },
                    placeholder = { Text(advice.text) },
                    modifier = Modifier.fieldWidth(texts.common.amount, Sizes.fieldAmount)
                )
                FieldButton(
                    text = texts.cash.deposit,
                    kind = FieldButtonKind.Filled,
                    enabled = ready && deposit is CashDecision.Ready
                ) { ask(CashMove.Deposit, deposit) }
                FieldButton(
                    text = texts.cash.withdraw,
                    enabled = ready && withdraw is CashDecision.Ready
                ) { ask(CashMove.Withdraw, withdraw) }
            }
        }
    }

    asked?.let { pending ->
        ConfirmCash(
            money = money.drawer,
            pending = pending,
            drawer = drawer,
            busy = busy,
            onCancel = { asked = null },
            onConfirm = {
                busy = true
                scope.launch {
                    val done = onSubmit(pending.move, pending.amount, pending.key)
                    busy = false
                    asked = null
                    if (done) {
                        // Ключ отпускается вместе с проведёнными деньгами:
                        // следующая такая же сумма — уже другая операция.
                        attempt = null
                        amount = ""
                    }
                }
            }
        )
    }
}
