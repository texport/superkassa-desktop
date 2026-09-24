package kz.mybrain.superkassa.presentation.kassa.refund.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.domain.kassa.model.refund.RefundDraft
import kz.mybrain.superkassa.presentation.common.list.ScrollableColumn
import kz.mybrain.superkassa.presentation.common.state.EmptyState
import kz.mybrain.superkassa.presentation.common.strings.LocalLanguage
import kz.mybrain.superkassa.presentation.kassa.contact.BuyerContactFields
import kz.mybrain.superkassa.presentation.kassa.refund.ReturnsActions
import kz.mybrain.superkassa.presentation.kassa.refund.ReturnsUiState
import kz.mybrain.superkassa.presentation.theme.icon.AppIcons
import kz.mybrain.superkassa.presentation.theme.size.Spacing
import kz.mybrain.superkassa.strings.api.journal.ReturnJournalTexts
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Сумма возврата и само действие.
 *
 * Панель поднята над списком: она — то, ради чего экран открыт, и одно
 * главное действие живёт здесь. Поле заполняется суммой чека целиком —
 * это обычный случай, — но менять её можно: покупатель возвращает один
 * товар из трёх. Больше суммы чека вернуть нельзя, и говорится об этом
 * до отправки.
 */
@Composable
fun RefundPanel(state: ReturnsUiState, actions: ReturnsActions, modifier: Modifier) {
    val journal = textsOf(LocalLanguage.current).journal.returns
    val draft = state.refund?.takeIf { state.basis != null }
    ElevatedCard(modifier = modifier.fillMaxHeight()) {
        if (draft == null) {
            EmptyState(
                icon = AppIcons.noBasis,
                title = journal.chooseBasis,
                hint = journal.chooseBasisHint,
                modifier = Modifier.fillMaxHeight()
            )
        } else {
            RefundForm(state, draft, actions, journal)
        }
    }
}

/**
 * Ввод суммы и отправка.
 *
 * Сумма и виды оплаты стоят над составом чека, а не под ним: у чека
 * из пятидесяти строк их на экране не было, а кнопка оставалась
 * нажимаемой. Кнопка прибита к низу.
 */
@Composable
private fun RefundForm(
    state: ReturnsUiState,
    draft: RefundDraft,
    actions: ReturnsActions,
    journal: ReturnJournalTexts
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.cardPadding),
        verticalArrangement = Arrangement.spacedBy(Spacing.fieldGap)
    ) {
        ScrollableColumn(modifier = Modifier.weight(1f), spacing = Spacing.fieldGap) {
            RefundSummary(draft.basis, journal, actions.basis::back)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            RefundTill {
                RefundNote(journal.itemsIgnored.takeIf { draft.chosen.isNotEmpty() && !draft.byLines })
                RefundAmountRow(journal, draft, actions.refund)
                RefundMoney(state, draft, actions.payments, journal)
                BuyerContactFields(draft.contact, state.channels, actions.refund::contactKind, actions.refund::contact)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            RefundItems(state, draft, journal, actions.refund::toggle)
        }
        RefundButton(state.kind, enabled = state.canRefund, onClick = actions.refund::refund)
        if (state.confirming) RefundConfirm(state.kind, draft, state.working, actions.refund)
    }
}
