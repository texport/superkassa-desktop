package kz.mybrain.superkassa.desktop.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons

/**
 * Строка поиска по списку — одна на всё приложение.
 *
 * Искать кассу на входе, точку в кабинете, документ в журнале и запись
 * в журнале приложения — одно и то же действие, и выглядеть оно обязано
 * одинаково. Написанная в каждом разделе заново, строка разъезжалась:
 * где-то со значком, где-то без, где-то с кнопкой очистки, где-то
 * набранное слово оставалось в поле навсегда.
 *
 * @param label чего ищем: подпись поля.
 * @param hint пример запроса под подписью; `null` — примера нет.
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
        label = { Text(label) },
        placeholder = hint?.let { { Text(it) } },
        leadingIcon = { Icon(icon, contentDescription = null) },
        trailingIcon = { ClearButton(value, clearLabel, onChange) },
        modifier = modifier
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
