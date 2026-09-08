package kz.mybrain.superkassa.desktop.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Плитка счётчика: число крупно, подпись под ним мелко.
 *
 * Счётчики стояли строками «подпись — значение» вперемешку с реквизитами
 * и читались как ещё одно поле карточки. Число здесь и есть содержание,
 * поэтому оно набрано шкалой заголовка, а подпись уведена в служебную.
 */
@Composable
fun CounterTile(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.widthIn(min = Sizes.counterTile),
        verticalArrangement = Arrangement.spacedBy(Spacing.hairline)
    ) {
        Text(text = value, style = MaterialTheme.typography.headlineSmall)
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
