package kz.mybrain.superkassa.presentation.common.document

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow
import kz.mybrain.superkassa.presentation.strings.common.LocalStrings
import kz.mybrain.superkassa.presentation.theme.icon.AppIcons
import kz.mybrain.superkassa.presentation.theme.icon.Glyphs

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

/**
 * Общее для журнала, возврата и очереди.
 *
 * Три экрана — один товароучётный журнал, разрезанный по задачам кассира,
 * и повторять в каждом тире и разбор времени незачем. Ожидание, пустота
 * и отказ здесь не живут: они одни на всё приложение и лежат в общем
 * `ScreenSlot`.
 */

/** Подложка строки журнала: через одну, чтобы глаз не терял строку. */
@Composable
fun rowTint(striped: Boolean) =
    if (striped) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.surface

/**
 * Значение, которого нет.
 *
 * Своего знака у журнала нет: прочерк один на всё приложение и объявлен
 * в [Glyphs]. Имя остаётся здесь, пока на него ссылается сводка; когда
 * и она перейдёт на общий знак, строка уйдёт.
 */
const val DASH: String = Glyphs.DASH
