package kz.mybrain.superkassa.desktop.ui.returns

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.server.Document
import kz.mybrain.superkassa.desktop.ui.components.fieldWidth
import kz.mybrain.superkassa.desktop.ui.history.DayBar
import kz.mybrain.superkassa.desktop.ui.strings.HistoryJournalTexts
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import java.time.LocalDate

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
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.normal),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DayBar(history, day, loading, onDay)
        OutlinedTextField(
            value = number,
            onValueChange = { onNumber(it.filter(Char::isDigit)) },
            label = { Text(history.colNumber) },
            singleLine = true,
            modifier = Modifier.fieldWidth(history.colNumber, Sizes.fieldPin)
        )
    }
}

/**
 * Подходит ли документ под набранный номер.
 *
 * Совпадение по вхождению, а не по началу: кассир набирает последние
 * цифры с чека, не переписывая номер целиком.
 */
internal fun Document.matches(typed: String): Boolean {
    val wanted = typed.trim()
    if (wanted.isEmpty()) return true
    // Искать можно и по номеру чека, и по фискальному признаку: на чеке
    // покупателя стоят оба, и кассир набирает то, что видит.
    return listOfNotNull(number?.toString(), fiscalSign, autonomousSign)
        .any { it.contains(wanted) }
}
