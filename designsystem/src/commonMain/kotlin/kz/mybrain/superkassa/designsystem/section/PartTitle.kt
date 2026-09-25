package kz.mybrain.superkassa.designsystem.section

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.designsystem.tip.InfoTip

/**
 * Подпись строки выбора внутри группы настроек — «Язык чека», «Копии»,
 * «Уровень» — и объяснение к ней, если есть.
 *
 * Набрана как подпись строки списка Material 3 (`bodyLarge`, `onSurface`),
 * а не как заголовок: заголовок в группе один — её подзаголовок цветом
 * `primary`. Прежде подпись была набрана `titleSmall` и читалась вторым
 * видом заголовка рядом с подзаголовком другого цвета: «Оформление»
 * одним цветом, «Тон» и «Шрифт» под ним — другим.
 *
 * Строка всегда высотой с цель нажатия, со значком подсказки или без
 * него: значок ⓘ — кнопка, и Material отводит ей 48 точек, а части одной
 * группы должны стоять с одинаковыми промежутками.
 */
@Composable
fun PartTitle(title: String, info: String? = null) {
    Row(
        modifier = Modifier.heightIn(min = LocalMinimumInteractiveComponentSize.current),
        horizontalArrangement = Arrangement.spacedBy(Spacing.buttonGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f, fill = false)
        )
        info?.let { InfoTip(it) }
    }
}
