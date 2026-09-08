package kz.mybrain.superkassa.desktop.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.Dictionary
import kz.mybrain.superkassa.desktop.server.Document
import kz.mybrain.superkassa.desktop.ui.components.DeliveryChip
import kz.mybrain.superkassa.desktop.ui.components.EmptyState
import kz.mybrain.superkassa.desktop.ui.components.Money
import kz.mybrain.superkassa.desktop.ui.components.ScrollableList
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.MoneyStyle
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Документы текущей смены.
 *
 * Список, а не карточка на строку: за смену их сотни, и рамка вокруг
 * каждой превращает журнал в лоскутное одеяло. Плотность та же, что
 * в разделе истории, — кассир читает обе таблицы одинаково.
 */
@Composable
fun ShiftDocuments(session: Session) {
    val texts = LocalStrings.current

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.snug)) {
        Text(texts.dashboard.shiftDocuments, style = MaterialTheme.typography.titleMedium)
        if (session.documents.isEmpty()) {
            EmptyState(
                icon = AppIcons.history,
                title = texts.dashboard.shiftDocuments,
                hint = if (session.shiftOpen) texts.sale.receipt else texts.dashboard.openShiftHint
            )
            return@Column
        }
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            ScrollableList {
                items(session.documents) { document ->
                    DocumentRow(
                        document = document,
                        documentTitle = session.titleOf(Dictionary.DocumentTypes, document.docType),
                        onPreview = { session.printDesk.preview(document) },
                        onPrint = { session.printDesk.print(document) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DocumentRow(
    document: Document,
    documentTitle: String,
    onPreview: () -> Unit,
    onPrint: () -> Unit
) {
    val texts = LocalStrings.current
    ListItem(
        headlineContent = { Text(documentTitle) },
        supportingContent = { Text(document.docNo?.toString() ?: DASH) },
        trailingContent = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(Money.formatTiyn(document.totalAmount), style = MoneyStyle.row)
                DeliveryChip(document.ofdStatus, document.isAutonomous == true, document.docType)
                // Код отказа вместо кнопки повтора: документ, который ОФД
                // отверг, повторной отправкой не исправить — операцию нужно
                // провести заново. Кассиру полезен не повтор, а причина.
                if (document.ofdErrorCode != null) {
                    Text(
                        text = "${texts.common.refusalCode} ${document.ofdErrorCode}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                // Два действия, а не одно: «просмотр» показывает форму
                // на экране, «печать» отправляет её на принтер рабочего
                // места. Раньше кнопка называлась печатью, а печатала
                // только в окно.
                // У отклонённого ОФД документа печатной формы нет вовсе:
                // она выглядит как настоящий чек, а фискальным он не стал.
                IconButton(enabled = document.printable, onClick = onPreview) {
                    Icon(AppIcons.preview, contentDescription = texts.preview.title)
                }
                IconButton(enabled = document.printable, onClick = onPrint) {
                    Icon(AppIcons.print, contentDescription = texts.preview.print)
                }
            }
        }
    )
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

/** Прочерк вместо пустого места: пустая клетка читается как недосмотр. */
private const val DASH = "—"
