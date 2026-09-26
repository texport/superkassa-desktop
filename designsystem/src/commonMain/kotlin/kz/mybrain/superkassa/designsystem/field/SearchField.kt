package kz.mybrain.superkassa.designsystem.field

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons

/**
 * Строка поиска по списку — одна на всё приложение.
 *
 * Искать кассу на входе, точку в кабинете, документ в журнале и запись
 * в журнале приложения — одно и то же действие, и выглядеть оно обязано
 * одинаково. Написанная в каждом разделе заново, строка разъезжалась:
 * где-то со значком, где-то без, где-то с кнопкой очистки, где-то
 * набранное слово оставалось в поле навсегда.
 *
 * Подписи над рамкой у строки поиска нет, как у поиска Material 3: что
 * ищем, сказано в самом поле, пока оно пустое. Поднятая подпись держала
 * над рамкой запас, и строка поиска вставала выше плашек отбора в том же
 * ряду; плашки отбора того же роста, что рамка ([kz.mybrain.superkassa
 * .designsystem.picker.SieveChip]), и ряд стоит ровно.
 *
 * @param label чего ищем: текст в пустом поле и название поля для чтения
 *   с экрана.
 * @param hint пример запроса — пишется в пустом поле вслед за [label];
 *   `null` — примера нет.
 * @param icon значок слева; по умолчанию лупа, но список касс помечен
 *   значком кассы — там ищут её, а не строку текста.
 * @param clearLabel подпись кнопки очистки; `null` — очистки нет.
 */
@Composable
fun SearchField(
    value: String,
    label: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
    icon: ImageVector = AppIcons.find,
    clearLabel: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        placeholder = { Text(hint?.let { "$label: $it" } ?: label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        trailingIcon = { ClearButton(value, clearLabel, onChange) },
        modifier = modifier.semantics { contentDescription = label }
    )
}

/**
 * Стереть набранное одним нажатием.
 *
 * Забытое в строке слово выглядит как пропавшие записи: список пуст,
 * а причина — вверху экрана мелким шрифтом.
 */
@Composable
private fun ClearButton(value: String, clearLabel: String?, onChange: (String) -> Unit) {
    if (clearLabel == null || value.isEmpty()) return
    IconButton(onClick = { onChange("") }) {
        Icon(AppIcons.close, contentDescription = clearLabel)
    }
}
