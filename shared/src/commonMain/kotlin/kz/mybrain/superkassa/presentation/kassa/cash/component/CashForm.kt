package kz.mybrain.superkassa.presentation.kassa.cash.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.domain.kassa.model.cash.CashDecision
import kz.mybrain.superkassa.domain.kassa.model.cash.CashMove
import kz.mybrain.superkassa.presentation.common.adaptive.WrapRow
import kz.mybrain.superkassa.presentation.common.button.FieldButton
import kz.mybrain.superkassa.presentation.common.button.FieldButtonKind
import kz.mybrain.superkassa.presentation.common.field.MoneyField
import kz.mybrain.superkassa.presentation.common.field.fieldMinWidth
import kz.mybrain.superkassa.presentation.kassa.cash.CashActions
import kz.mybrain.superkassa.presentation.kassa.cash.CashUiState
import kz.mybrain.superkassa.presentation.strings.common.LocalStrings
import kz.mybrain.superkassa.presentation.strings.kassa.MoneyTexts
import kz.mybrain.superkassa.presentation.theme.size.Sizes
import kz.mybrain.superkassa.presentation.theme.size.Spacing

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
internal fun CashForm(state: CashUiState, actions: CashActions, money: MoneyTexts) {
    val advice = adviceOn(state.holdup, money.drawer, state.cashInDrawer)
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.roomy),
            verticalArrangement = Arrangement.spacedBy(Spacing.snug)
        ) {
            CashInput(state, actions, advice)
            // Помеха — строкой во всю ширину карточки, а не подписью поля.
            (advice as? CashAdvice.Holdup)?.let { Holdup(it) }
        }
    }
    state.asked?.let { pending ->
        ConfirmCash(money.drawer, pending, state.cashInDrawer, state.busy, actions::cancel, actions::confirm)
    }
}

/**
 * Сумма и оба действия — одной строкой и одного роста: кнопка под полем
 * читается как отдельный блок, хотя это одно действие — «внести столько-то».
 * Не помещаются — кнопки переносятся под поле целиком, а не сжимаются.
 */
@Composable
private fun CashInput(state: CashUiState, actions: CashActions, advice: CashAdvice) {
    val texts = LocalStrings.current
    WrapRow(modifier = Modifier.fillMaxWidth(), spacing = Spacing.snug) {
        MoneyField(
            value = state.amount,
            label = texts.common.amount,
            modifier = Modifier.weight(1f).fieldMinWidth(texts.common.amount, Sizes.fieldAmount),
            isError = (advice as? CashAdvice.Holdup)?.mistake == true,
            // Правило ввода — подсказкой в самом поле.
            placeholder = (advice as? CashAdvice.Hint)?.text,
            onValueChange = actions::enter
        )
        FieldButton(
            text = texts.cash.deposit,
            kind = FieldButtonKind.Filled,
            enabled = state.ready && state.decision(CashMove.Deposit) is CashDecision.Ready
        ) { actions.ask(CashMove.Deposit) }
        FieldButton(
            text = texts.cash.withdraw,
            enabled = state.ready && state.decision(CashMove.Withdraw) is CashDecision.Ready
        ) { actions.ask(CashMove.Withdraw) }
    }
}

/**
 * Что мешает провести деньги.
 *
 * Ошибка в набранном окрашена ролью отказа, состояние кассы — приглушённой
 * ролью поверхности: красное «Смена закрыта» кассир читает как поломку,
 * а смену просто ещё не открыли.
 */
@Composable
private fun Holdup(holdup: CashAdvice.Holdup) {
    Text(
        text = holdup.text,
        style = MaterialTheme.typography.bodySmall,
        color = if (holdup.mistake) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = Modifier.fillMaxWidth()
    )
}
