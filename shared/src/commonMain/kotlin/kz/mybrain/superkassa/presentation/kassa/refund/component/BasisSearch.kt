package kz.mybrain.superkassa.presentation.kassa.refund.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.datetime.LocalDate
import kz.mybrain.superkassa.presentation.common.period.DayBar
import kz.mybrain.superkassa.presentation.strings.journal.HistoryJournalTexts
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
    // Номер тянется на остаток ряда: его края — края списка чеков под ним.
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.cardGap), verticalAlignment = Alignment.CenterVertically) {
        DayBar(history, day, loading, onDay)
        OutlinedTextField(
            value = number,
            onValueChange = onNumber,
            label = { Text(history.colNumber) },
            singleLine = true,
            // Номер чека — цифры: на планшете открывается цифровая клавиатура.
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f)
        )
    }
}
