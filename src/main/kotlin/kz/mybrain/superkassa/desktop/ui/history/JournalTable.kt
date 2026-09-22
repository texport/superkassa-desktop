package kz.mybrain.superkassa.desktop.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import kz.mybrain.superkassa.desktop.ui.strings.HistoryJournalTexts
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs
import kz.mybrain.superkassa.desktop.ui.theme.MoneyStyle
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Таблица журнала документов: шапка и строка.
 *
 * Одна на журнал кассы и на документы кассы в кабинете: столбцы, их
 * ширины и место кнопок печати заданы здесь, а не на каждом экране.
 * Написанные дважды, они разъезжались — сумма стояла в разных столбцах
 * у одного и того же чека.
 *
 * Шапка стоит над списком, а не в нём: при прокрутке тысячи строк смены
 * подписи столбцов обязаны оставаться на месте.
 */
@Composable
fun JournalHeader(journal: HistoryJournalTexts) {
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
            HeadCell(journal.colShift, SHIFT, TextAlign.End)
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
 * @param onOpen что показать по нажатию на строку; `null` — строка
 *   не нажимается, и указатель это показывает.
 * @param onPreview показ печатной формы; `null` — формы у источника нет.
 * @param onPrint отправка формы на принтер; `null` — печатать нечем.
 */
@Composable
fun JournalRow(
    entry: JournalEntry,
    striped: Boolean,
    onOpen: (() -> Unit)? = null,
    onPreview: (() -> Unit)? = null,
    onPrint: (() -> Unit)? = null
) {
    val base = Modifier.fillMaxWidth().background(rowTint(striped))
    Row(
        modifier = (if (onOpen == null) base else base.clickable(onClick = onOpen))
            .padding(horizontal = Spacing.normal),
        horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BodyCell(entry.moment, TIME)
        BodyCell(entry.type, TYPE)
        BodyCell(entry.number, NUMBER, TextAlign.End)
        BodyCell(entry.shiftNo?.toString() ?: Glyphs.DASH, SHIFT, TextAlign.End)
        // Сумма — моноширинно и вправо: столбец читается сверху вниз
        // одним движением глаза, а не выискивается по ширине знаков.
        Text(
            text = entry.amount,
            style = MoneyStyle.row,
            maxLines = 1,
            modifier = Modifier.weight(AMOUNT)
        )
        BodyCell(entry.sign, SIGN)
        Box(modifier = Modifier.weight(STATE)) {
            // О доставке говорит не всякая запись: у смены состояние своё —
            // открыта она или закрыта.
            if (entry.delivery != null) {
                JournalDeliveryChip(entry.delivery, entry.refusal)
            } else {
                JournalStateChip(entry.state)
            }
        }
        Box(modifier = Modifier.weight(PRINT), contentAlignment = Alignment.CenterEnd) {
            RowActions(entry, onPreview, onPrint)
        }
    }
}

/**
 * Показать форму на экране и отправить её на принтер.
 *
 * Печатной формы у отклонённого документа нет: фиктивный чек на руках
 * покупателя дороже любого удобства.
 */
@Composable
private fun RowActions(entry: JournalEntry, onPreview: (() -> Unit)?, onPrint: (() -> Unit)?) {
    val texts = LocalStrings.current
    Row {
        if (onPreview != null) {
            IconButton(enabled = entry.printable, onClick = onPreview) {
                Icon(AppIcons.preview, contentDescription = texts.preview.title)
            }
        }
        if (onPrint != null) {
            IconButton(enabled = entry.printable, onClick = onPrint) {
                Icon(AppIcons.print, contentDescription = texts.preview.print)
            }
        }
    }
}

/** Подпись столбца: мельче и тише строки, иначе шапка спорит с данными. */
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
private const val SHIFT = 0.5f
private const val AMOUNT = 1.1f
private const val SIGN = 1.5f
private const val STATE = 0.9f
private const val PRINT = 0.5f
