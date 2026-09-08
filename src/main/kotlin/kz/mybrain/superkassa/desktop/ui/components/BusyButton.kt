package kz.mybrain.superkassa.desktop.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Главное действие, которое ждёт ответа узла.
 *
 * Пока идёт обращение, кнопка держит на себе кружок ожидания и не
 * принимает повторного нажатия. Без этого экран в момент ожидания
 * неотличим от непринятого нажатия, и кассир жмёт второй раз.
 *
 * Кружок стоит внутри кнопки, а не вместо неё: подпись остаётся на месте,
 * и кнопка не меняет размер под курсором.
 *
 * @param text подпись действия.
 * @param busy идёт ли обращение прямо сейчас.
 * @param enabled разрешено ли действие по существу.
 */
@Composable
fun BusyButton(
    text: String,
    busy: Boolean,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = Spacing.normal, vertical = Spacing.snug),
    onClick: () -> Unit
) {
    Button(
        enabled = enabled && !busy,
        onClick = onClick,
        contentPadding = contentPadding,
        modifier = modifier
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (busy) {
                CircularProgressIndicator(
                    strokeWidth = Sizes.busyLine,
                    color = LocalContentColor.current,
                    modifier = Modifier.size(Sizes.busyCircle)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
