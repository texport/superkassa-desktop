package kz.mybrain.superkassa.desktop.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Пустое состояние по Material 3.
 *
 * Пустой экран обязан сказать три вещи: что здесь бывает, почему сейчас
 * пусто и что сделать. Голая строка «нет данных» оставляет кассира гадать,
 * сломалось что-то или так и должно быть.
 *
 * Одно на всё приложение. Своих было три — у журнала, у кассиров
 * и у движения денег, — и они разошлись: разный значок по размеру,
 * разный оттенок, разные отступы. Внутри карточки или плотного списка
 * то же состояние показывается [dense]: значок мельче, воздуха меньше.
 *
 * @param dense плотный вид для списка внутри карточки.
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    hint: String? = null,
    modifier: Modifier = Modifier,
    dense: Boolean = false
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(Spacing.roomy),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (dense) Spacing.tight else Spacing.snug)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(if (dense) Sizes.emptyIconDense else Sizes.emptyIcon)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        if (hint != null) {
            Text(
                text = hint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
