package kz.mybrain.superkassa.presentation.setup.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.theme.StatusColors
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.designsystem.theme.size.Spacing

/**
 * Итог сделанного шага: что сделано и главное число шага.
 *
 * Число — заводской номер, номер КГД — набрано крупно шкалой `headline`:
 * его переписывают или диктуют, и оно выделяется, чтобы его можно было
 * скопировать. Отметка «готово» — цветом состояния «доставлено», как
 * у отправленного в БФД: зелёная отметка читается «сделано» без слов.
 *
 * @param what что сделано: «Касса на учёте»; `null` — шаг называет заголовок.
 * @param value главное число шага; `null` — числа у шага нет.
 * @param note строка под числом: год выпуска, идентификатор кассы.
 * @param doneLabel слово «готово» для чтения с экрана.
 */
@Composable
internal fun StepDone(what: String?, value: String?, note: String?, doneLabel: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.fieldGap),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = AppIcons.stepDone,
            contentDescription = doneLabel,
            tint = StatusColors.delivered
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.inline)) {
            if (what != null) Text(what, style = MaterialTheme.typography.titleMedium)
            if (value != null) {
                SelectionContainer { Text(value, style = MaterialTheme.typography.headlineMedium) }
            }
            if (note != null) {
                val color = MaterialTheme.colorScheme.onSurfaceVariant
                Text(note, style = MaterialTheme.typography.bodyMedium, color = color)
            }
        }
    }
}
