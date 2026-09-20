package kz.mybrain.superkassa.desktop.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.app.titleOf
import kz.mybrain.superkassa.desktop.server.Dictionary
import kz.mybrain.superkassa.desktop.server.Document
import kz.mybrain.superkassa.desktop.ui.components.DeliveryChip
import kz.mybrain.superkassa.desktop.ui.components.Money
import kz.mybrain.superkassa.desktop.ui.components.RecordRow
import kz.mybrain.superkassa.desktop.ui.components.ScreenSlot
import kz.mybrain.superkassa.desktop.ui.components.ScreenState
import kz.mybrain.superkassa.desktop.ui.components.ScrollableList
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs
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
        // Список приходит вместе с состоянием смены: до ответа узла пустота
        // читалась как «за смену не пробито ничего».
        val state = when {
            session.documents.isNotEmpty() -> ScreenState.Ready
            session.busy -> ScreenState.Working
            else -> ScreenState.Empty(
                icon = AppIcons.history,
                title = texts.dashboard.shiftDocuments,
                hint = if (session.shiftOpen) texts.sale.receipt else texts.dashboard.openShiftHint
            )
        }
        ScreenSlot(state, dense = true) {
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
}

@Composable
private fun DocumentRow(
    document: Document,
    documentTitle: String,
    onPreview: () -> Unit,
    onPrint: () -> Unit
) {
    val texts = LocalStrings.current
    RecordRow(
        title = documentTitle,
        subtitle = document.docNo?.toString() ?: Glyphs.DASH,
        amount = Money.formatTiyn(document.totalAmount),
        trailing = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DeliveryChip(document.ofdStatus, document.isAutonomous == true, document.docType)
                // Код отказа вместо кнопки повтора: документ, который ОФД
                // отверг, повторной отправкой не исправить — операцию нужно
                // провести заново. Кассиру полезен не повтор, а причина.
                if (document.refusalCode != null) {
                    Text(
                        text = "${texts.common.refusalCode} ${document.refusalCode}",
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
