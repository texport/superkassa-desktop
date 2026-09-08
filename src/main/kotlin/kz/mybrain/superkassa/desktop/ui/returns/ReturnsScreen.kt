package kz.mybrain.superkassa.desktop.ui.returns

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.Document
import kz.mybrain.superkassa.desktop.ui.components.ChoiceSegments
import kz.mybrain.superkassa.desktop.ui.components.DeliveryChip
import kz.mybrain.superkassa.desktop.ui.components.InfoTip
import kz.mybrain.superkassa.desktop.ui.components.Money
import kz.mybrain.superkassa.desktop.ui.history.JournalEmpty
import kz.mybrain.superkassa.desktop.ui.history.JournalIcons
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.ReturnJournalTexts
import kz.mybrain.superkassa.desktop.ui.strings.journalTexts
import kz.mybrain.superkassa.desktop.ui.theme.MoneyStyle
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Возврат по чеку-основанию.
 *
 * Две колонки: слева чеки открытой смены, годные в основание, справа —
 * сумма возврата и единственное главное действие экрана. Возврат
 * по возврату протокол не допускает, и такие чеки в список не попадают:
 * кассир не должен узнавать о запрете из отказа ОФД.
 */
@Composable
fun ReturnsScreen(session: Session) {
    val journal = journalTexts(session.language).returns
    var kind by remember { mutableStateOf(ReturnKind.Sell) }
    var basisId by remember { mutableStateOf<String?>(null) }

    val candidates = kind.basisIn(session.documents)
    val chosen = candidates.firstOrNull { it.id == basisId }

    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.screen),
        verticalArrangement = Arrangement.spacedBy(Spacing.normal)
    ) {
        ReturnHeader(journal, kind) {
            kind = it
            basisId = null
        }
        when {
            // Закрытая смена — состояние, а не отказ: об этом сказано словами
            // и подсказкой, а не пустым списком, из которого ничего не понять.
            !session.shiftOpen -> JournalEmpty(
                icon = JournalIcons.noBasis,
                line = journal.shiftClosed,
                hint = journal.shiftClosedHint,
                modifier = Modifier.weight(1f)
            )
            candidates.isEmpty() -> JournalEmpty(
                icon = JournalIcons.noBasis,
                line = kind.emptyText(journal),
                hint = journal.noBasisHint,
                modifier = Modifier.weight(1f)
            )
            else -> Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(Spacing.normal)
            ) {
                BasisList(
                    candidates = candidates,
                    chosen = chosen,
                    journal = journal,
                    modifier = Modifier.weight(BASIS_COLUMN)
                ) { basisId = it.id }
                RefundPanel(session, kind, chosen, Modifier.weight(REFUND_COLUMN)) { basisId = null }
            }
        }
    }
}

/**
 * Заголовок экрана и направление возврата.
 *
 * Направлений два и они исключают друг друга — это выбор одного из двух,
 * а не отбор, поэтому сегменты, а не плашки: возврат продажи выдаёт деньги
 * покупателю, возврат покупки принимает их обратно, и путать их нельзя.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColumnScope.ReturnHeader(
    journal: ReturnJournalTexts,
    kind: ReturnKind,
    onKind: (ReturnKind) -> Unit
) {
    val texts = LocalStrings.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.normal),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(texts.returns.title, style = MaterialTheme.typography.headlineSmall)
        // В сегменте стоит только направление: «Возврат» уже написано
        // заголовком слева.
        ChoiceSegments(
            options = ReturnKind.entries,
            selected = kind,
            label = { it.shortTitle(texts.sale) },
            onSelect = onKind
        )
        // Правило возврата — под значком: кассир читает его один раз,
        // а место на экране оно занимало бы в каждой смене.
        InfoTip(journal.basisHint)
    }
}

/** Перечень чеков-оснований: список в карточке, выбранный выделен подложкой. */
@Composable
private fun BasisList(
    candidates: List<Document>,
    chosen: Document?,
    journal: ReturnJournalTexts,
    modifier: Modifier,
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
    ListItem(
        headlineContent = {
            Text(
                text = "${texts.returns.receiptNo} ${candidate.docNo}",
                style = MaterialTheme.typography.titleSmall
            )
        },
        supportingContent = {
            // Узел хранит фискальный признак и номером документа: писать
            // одно и то же число дважды подряд незачем.
            val sign = candidate.fiscalSign ?: candidate.autonomousSign
            if (sign != null && sign != candidate.docNo?.toString()) {
                Text(
                    text = "${journal.fiscalSign}: $sign",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        trailingContent = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(Spacing.hairline)
            ) {
                Text(Money.formatTiyn(candidate.totalAmount), style = MoneyStyle.row)
                DeliveryChip(candidate.ofdStatus, candidate.isAutonomous == true)
            }
        },
        colors = ListItemDefaults.colors(containerColor = basisTint(selected)),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onChoose)
    )
}

/** Выбранный чек выделен ролью «всё хорошо», а не собственной краской. */
@Composable
private fun basisTint(selected: Boolean) =
    if (selected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }

/** Доли ширины: список чеков шире панели, в нём читают, а не вводят. */
private const val BASIS_COLUMN = 1.3f
private const val REFUND_COLUMN = 1f
