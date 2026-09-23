package kz.mybrain.superkassa.presentation.returns

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.data.node.Document
import kz.mybrain.superkassa.presentation.adaptive.MoneyText
import kz.mybrain.superkassa.presentation.adaptive.WrapRow
import kz.mybrain.superkassa.presentation.components.Money
import kz.mybrain.superkassa.presentation.components.MoreRow
import kz.mybrain.superkassa.presentation.components.RecordRow
import kz.mybrain.superkassa.presentation.history.DocumentDeliveryChip
import kz.mybrain.superkassa.presentation.strings.HistoryJournalTexts
import kz.mybrain.superkassa.presentation.strings.LocalStrings
import kz.mybrain.superkassa.presentation.strings.ReturnJournalTexts
import kz.mybrain.superkassa.presentation.theme.Spacing

/** Перечень чеков-оснований: список в карточке, выбранный выделен подложкой. */
@Composable
internal fun BasisList(
    candidates: List<Document>,
    chosen: Document?,
    journal: ReturnJournalTexts,
    history: HistoryJournalTexts,
    more: Boolean,
    loading: Boolean,
    modifier: Modifier,
    onMore: () -> Unit,
    onChoose: (Document) -> Unit
) {
    Card(
        modifier = modifier.fillMaxHeight(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Text(
            text = "${journal.basisColumn}: ${candidates.size}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.normal, vertical = Spacing.snug)
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        LazyColumn {
            items(candidates) { candidate ->
                BasisRow(candidate, candidate.id == chosen?.id, journal) { onChoose(candidate) }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            // День читается страницами: за оживлённый день чеков сотни,
            // и тянуть их все ради одного основания незачем.
            item { MoreRow(more, loading, history.showMore, journal.allBasesShown, onMore = onMore) }
        }
    }
}

/**
 * Чек-основание строкой списка.
 *
 * Номер — заголовок во всю ширину строки, под ним сумма, фискальный
 * признак и состояние доставки. Признак виден до нажатия: по нему кассир
 * сверяет бумажный чек покупателя со строкой на экране. Сумма и плашка
 * доставки стояли справа от номера, и в узком списке номер и значение
 * признака обрезались многоточием; под номером они переносятся, а не
 * отнимают у них место.
 */
@Composable
private fun BasisRow(
    candidate: Document,
    selected: Boolean,
    journal: ReturnJournalTexts,
    onChoose: () -> Unit
) {
    val texts = LocalStrings.current
    // Номер — тот, что стоит на бумажном чеке покупателя: его касса
    // присваивает сама. Номер от ОФД совпадает с фискальным признаком,
    // и по нему кассир бумагу с экраном не сверит.
    val number = candidate.number
    val sign = (candidate.fiscalSign ?: candidate.autonomousSign)
        ?.takeIf { it != number?.toString() }
    RecordRow(
        title = "${texts.returns.receiptNo} $number",
        selected = selected,
        onClick = onChoose,
        support = {
            WrapRow {
                MoneyText(Money.formatTiyn(candidate.totalAmount))
                sign?.let {
                    Text(
                        text = "${journal.fiscalSign}: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DocumentDeliveryChip(candidate)
            }
        }
    )
}
