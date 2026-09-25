package kz.mybrain.superkassa.presentation.kassa.refund.component

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
import androidx.compose.ui.Modifier
import io.github.texport.superkassa.core.presentation.api.model.kkm.FiscalDocumentResponse
import kz.mybrain.superkassa.designsystem.adaptive.WrapRow
import kz.mybrain.superkassa.designsystem.list.RecordRow
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.designsystem.text.MoneyText
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.domain.document.model.number
import kz.mybrain.superkassa.presentation.common.document.DocumentDeliveryChip
import kz.mybrain.superkassa.presentation.common.format.Money
import kz.mybrain.superkassa.strings.api.journal.ReturnJournalTexts

/** Перечень чеков-оснований: список в карточке, выбранный выделен подложкой. */
@Composable
internal fun BasisList(
    candidates: List<FiscalDocumentResponse>,
    chosen: FiscalDocumentResponse?,
    journal: ReturnJournalTexts,
    modifier: Modifier,
    onChoose: (FiscalDocumentResponse) -> Unit
) {
    Card(
        modifier = modifier.fillMaxHeight(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Text(
            text = "${journal.basisColumn}: ${candidates.size}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.cardPadding, vertical = Spacing.fieldGap)
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        LazyColumn {
            items(candidates) { candidate ->
                BasisRow(candidate, candidate.id == chosen?.id, journal) { onChoose(candidate) }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
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
    candidate: FiscalDocumentResponse,
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
