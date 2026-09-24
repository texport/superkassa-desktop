package kz.mybrain.superkassa.presentation.kassa.refund.component

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.datetime.LocalDate
import kz.mybrain.superkassa.presentation.common.adaptive.WrapRow
import kz.mybrain.superkassa.presentation.common.field.fieldWidth
import kz.mybrain.superkassa.presentation.common.period.DayBar
import kz.mybrain.superkassa.presentation.strings.journal.HistoryJournalTexts
import kz.mybrain.superkassa.presentation.theme.size.Sizes
import kz.mybrain.superkassa.presentation.theme.size.Spacing

/**
 * Поиск чека-основания: день и номер.
 *
 * Покупатель приходит с чеком в руках, и на чеке напечатан его номер —
 * это самый короткий путь к основанию. День перелистывается тем же
 * набором, что и в журнале: искать чек кассир должен одинаково,
 * откуда бы ни начал.
 */
@Composable
internal fun BasisSearch(
    history: HistoryJournalTexts,
    day: LocalDate,
    number: String,
    loading: Boolean,
    onNumber: (String) -> Unit,
    onDay: (LocalDate) -> Unit
) {
    // Ряд переносится: на узком окне поле номера уходит под перелистывание
    // дня, а не сжимается.
    WrapRow(spacing = Spacing.normal) {
        DayBar(history, day, loading, onDay)
        OutlinedTextField(
            value = number,
            onValueChange = onNumber,
            label = { Text(history.colNumber) },
            singleLine = true,
            modifier = Modifier.fieldWidth(history.colNumber, Sizes.fieldPin)
        )
    }
}
