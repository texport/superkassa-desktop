package kz.mybrain.superkassa.desktop.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.Document
import kz.mybrain.superkassa.desktop.ui.components.DeliveryChip
import kz.mybrain.superkassa.desktop.ui.components.Money
import kz.mybrain.superkassa.desktop.ui.strings.HistoryJournalTexts
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.MoneyStyle
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Шапка журнала.
 *
 * Стоит над списком, а не в нём: при прокрутке тысячи строк смены подписи
 * колонок обязаны оставаться на месте. Отделена от строк подложкой
 * и чертой — иначе первая строка читается как продолжение заголовка.
 */
@Composable
fun DocumentJournalHeader(journal: HistoryJournalTexts) {
    val texts = LocalStrings.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = Spacing.normal, vertical = Spacing.tight),
            horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeadCell(journal.colTime, TIME)
            HeadCell(journal.colType, TYPE)
            HeadCell(journal.colNumber, NUMBER, TextAlign.End)
            HeadCell(journal.colAmount, AMOUNT, TextAlign.End)
            HeadCell(journal.colFiscalSign, SIGN)
            HeadCell(texts.dashboard.state, STATE)
            HeadCell(texts.common.print, PRINT, TextAlign.End)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

/**
 * Строка журнала.
 *
 * Карточки под каждой строкой не осталось намеренно: они разносили сотню
 * чеков на три экрана прокрутки и рвали столбец сумм зазорами. Строки идут
 * вплотную, а разделяет их подложка через одну.
 *
 * Печать доступна у любой строки: печатную форму рисует узел, и это
 * единственный способ увидеть документ глазами — Z-отчёт закрытой смены
 * в том числе.
 */
@Composable
fun DocumentJournalRow(
    session: Session,
    document: Document,
    striped: Boolean,
    onPreview: () -> Unit,
    onPrint: () -> Unit
) {
    val texts = LocalStrings.current
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(rowTint(striped))
            .padding(horizontal = Spacing.normal),
        horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BodyCell(momentText(document.createdAt), TIME)
        BodyCell(documentTypeTitle(session, texts, document.docType), TYPE)
        BodyCell(document.docNo?.toString() ?: DASH, NUMBER, TextAlign.End)
        // Сумма — моноширинно и вправо: столбец читается сверху вниз
        // одним движением глаза, а не выискивается по ширине знаков.
        Text(
            text = Money.formatTiyn(document.totalAmount),
            style = MoneyStyle.row,
            maxLines = 1,
            modifier = Modifier.weight(AMOUNT)
        )
        // Автономный признак показан наравне с фискальным: у документа,
        // пробитого без связи, фискального признака ещё нет, и пустая
        // клетка выглядела бы утратой документа.
        BodyCell(document.fiscalSign ?: document.autonomousSign ?: DASH, SIGN)
        Box(modifier = Modifier.weight(STATE)) {
            DeliveryChip(document.ofdStatus, document.isAutonomous == true)
        }
        Box(modifier = Modifier.weight(PRINT), contentAlignment = Alignment.CenterEnd) {
            // Те же два действия, что и в документах смены: показать форму
            // на экране и отправить её на принтер.
            Row {
                // Печатной формы у отклонённого документа нет: фиктивный
                // чек на руках покупателя дороже любого удобства.
                IconButton(enabled = document.printable, onClick = onPreview) {
                    Icon(AppIcons.preview, contentDescription = texts.preview.title)
                }
                IconButton(enabled = document.printable, onClick = onPrint) {
                    Icon(AppIcons.print, contentDescription = texts.preview.print)
                }
            }
        }
    }
}

/** Подпись колонки: мельче и тише строки, иначе шапка спорит с данными. */
@Composable
private fun RowScope.HeadCell(text: String, weight: Float, align: TextAlign = TextAlign.Start) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = align,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(weight)
    )
}

/** Клетка строки: одна строка текста, обрезается многоточием, а не переносом. */
@Composable
private fun RowScope.BodyCell(text: String, weight: Float, align: TextAlign = TextAlign.Start) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        textAlign = align,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(weight)
    )
}

private const val TIME = 1f
private const val TYPE = 1.1f
private const val NUMBER = 0.6f
private const val AMOUNT = 1.1f
private const val SIGN = 1.5f
private const val STATE = 0.9f
private const val PRINT = 0.5f
