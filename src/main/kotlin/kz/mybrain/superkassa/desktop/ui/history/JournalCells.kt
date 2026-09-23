package kz.mybrain.superkassa.desktop.ui.history

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons

/*
 * Ячейки строки журнала, у которых есть своё поведение: плашка состояния,
 * кнопки показа и печати, название с многоточием.
 */

/** О доставке говорит не всякая запись: у смены состояние своё. */
@Composable
internal fun StateCell(entry: JournalEntry) {
    if (entry.delivery != null) {
        JournalDeliveryChip(entry.delivery, entry.refusal)
    } else {
        JournalStateChip(entry.state)
    }
}

/**
 * Показать форму на экране или отправить её на принтер.
 *
 * Печатной формы у отклонённого документа нет: фиктивный чек на руках
 * покупателя дороже любого удобства.
 */
@Composable
internal fun RowAction(entry: JournalEntry, onClick: (() -> Unit)?, preview: Boolean) {
    val action = onClick ?: return
    val texts = LocalStrings.current
    IconButton(enabled = entry.printable, onClick = action) {
        if (preview) {
            Icon(AppIcons.preview, contentDescription = texts.preview.title)
        } else {
            Icon(AppIcons.print, contentDescription = texts.preview.print)
        }
    }
}

/** Название в ячейке: одной строкой, длинное — с многоточием. */
@Composable
internal fun NameCell(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
}
