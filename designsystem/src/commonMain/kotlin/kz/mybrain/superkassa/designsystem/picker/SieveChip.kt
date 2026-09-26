package kz.mybrain.superkassa.designsystem.picker

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.designsystem.theme.size.Sizes

/**
 * Плашка отбора — `FilterChip` Material 3 ростом с рамку поля поиска.
 *
 * Отбор стоит в одном ряду со строкой поиска, и плашка своего роста
 * (32 точки) рядом с полем в 56 выглядела пришедшей с другого экрана.
 * Рост берётся у поля ([Sizes.fieldHeight]) — один на все отборы:
 * журнал, документы кассы, точки кабинета, карту касс.
 *
 * У нажатой плашки — галочка, как у плашки отбора Material 3: без неё
 * нажатое отличалось только заливкой.
 *
 * @param checked ставить ли галочку у выбранной; у плашки со списком
 *   её нет — выбранное там названо самой надписью.
 */
@Composable
fun SieveChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    checked: Boolean = true,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text = label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        modifier = modifier.height(Sizes.fieldHeight),
        enabled = enabled,
        leadingIcon = if (checked && selected) {
            { Icon(AppIcons.chosen, contentDescription = null, modifier = Modifier.size(Sizes.chipIcon)) }
        } else {
            null
        },
        trailingIcon = trailingIcon
    )
}
