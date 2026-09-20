package kz.mybrain.superkassa.desktop.ui.returns

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
import kz.mybrain.superkassa.desktop.server.Document
import kz.mybrain.superkassa.desktop.ui.components.DeliveryChip
import kz.mybrain.superkassa.desktop.ui.components.Money
import kz.mybrain.superkassa.desktop.ui.components.MoreRow
import kz.mybrain.superkassa.desktop.ui.components.RecordRow
import kz.mybrain.superkassa.desktop.ui.strings.HistoryJournalTexts
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.ReturnJournalTexts
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

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
            item { MoreRow(more, loading, history.showMore, history.allShown, onMore = onMore) }
        }
    }
}

/**
 * Чек-основание строкой списка.
 *
 * Номер — заголовок, фискальный признак — подпись под ним, сумма и состояние
 * доставки — справа. Признак виден до нажатия: по нему кассир сверяет
 * бумажный чек покупателя со строкой на экране.
 */
@Composable
private fun BasisRow(
    candidate: Document,
    selected: Boolean,
    journal: ReturnJournalTexts,
    onChoose: () -> Unit
) {
    val texts = LocalStrings.current
    // Узел хранит фискальный признак и номером документа: писать
    // одно и то же число дважды подряд незачем.
    val sign = (candidate.fiscalSign ?: candidate.autonomousSign)
        ?.takeIf { it != candidate.docNo?.toString() }
    RecordRow(
        title = "${texts.returns.receiptNo} ${candidate.docNo}",
        subtitle = sign?.let { "${journal.fiscalSign}: $it" },
        amount = Money.formatTiyn(candidate.totalAmount),
        selected = selected,
        onClick = onChoose,
        trailing = { DeliveryChip(candidate.ofdStatus, candidate.isAutonomous == true) }
    )
}
