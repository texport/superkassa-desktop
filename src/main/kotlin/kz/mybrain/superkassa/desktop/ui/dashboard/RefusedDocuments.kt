package kz.mybrain.superkassa.desktop.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.Dictionary
import kz.mybrain.superkassa.desktop.server.Document
import kz.mybrain.superkassa.desktop.server.documentDetails
import kz.mybrain.superkassa.desktop.ui.components.InfoTip
import kz.mybrain.superkassa.desktop.ui.components.Money
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.theme.MoneyStyle
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import kz.mybrain.superkassa.desktop.ui.theme.StatusColors

/**
 * Документы смены, которые ОФД отверг.
 *
 * В счётчики они не идут и фискальными не считаются — значит, в общем
 * списке документов они выглядят такими же строками, как принятые чеки,
 * и разобраться, что пошло не так, нечем. Здесь их число, причина отказа
 * и кто оформил: с этим уже можно идти к обслуживанию.
 *
 * Пока отказов нет, карточки нет вовсе: пустая строка «ошибок: 0» —
 * шум на главном экране.
 */
@Composable
internal fun RefusedDocuments(session: Session) {
    val texts = LocalStrings.current
    val refused = session.documents.filterNot { it.printable }
    if (refused.isEmpty()) return
    val operators = remember { mutableStateMapOf<String, String>() }

    // Кто оформил, узел отдаёт вместе с составом документа: в списке
    // документов этого поля нет, а без имени отказ — «кто-то в 14:53».
    LaunchedEffect(refused.map { it.id }) {
        val kkm = session.selected ?: return@LaunchedEffect
        refused.forEach { document ->
            if (operators.containsKey(document.id)) return@forEach
            val details = session.guard(texts.dashboard.refused) {
                session.client.documentDetails(kkm.kkmId, document.id, session.pin)
            }
            details?.operatorName?.let { operators[document.id] = it }
        }
    }

    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.normal),
            verticalArrangement = Arrangement.spacedBy(Spacing.tight)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${texts.dashboard.refused}: ${refused.size}",
                    style = MaterialTheme.typography.titleMedium,
                    color = StatusColors.refused
                )
                InfoTip(texts.dashboard.refusedHint)
            }
            refused.forEach { document ->
                RefusedRow(session, document, operators[document.id])
            }
        }
    }
}

/** Строка отказа: что за документ, на какую сумму, чей и по какой причине. */
@Composable
private fun RefusedRow(session: Session, document: Document, operator: String?) {
    val texts = LocalStrings.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = listOfNotNull(
                session.titleOf(Dictionary.DocumentTypes, document.docType),
                operator
            ).joinToString(SEPARATOR),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(Money.formatTiyn(document.totalAmount), style = MoneyStyle.row)
        Text(
            text = "${texts.common.refusalCode} ${document.ofdErrorCode ?: DASH}",
            style = MaterialTheme.typography.labelMedium,
            color = StatusColors.refused
        )
    }
}

/** Разделитель между видом документа и кассиром. */
private const val SEPARATOR = " · "

/** Прочерк там, где кода нет. */
private const val DASH = "—"
