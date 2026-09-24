package kz.mybrain.superkassa.presentation.common.section

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.presentation.common.message.InfoTip
import kz.mybrain.superkassa.presentation.theme.size.Spacing

/**
 * Название части карточки настроек — «Язык чека», «Копии», «Уровень» —
 * и объяснение к ней, если есть.
 *
 * Строка всегда высотой с цель нажатия, со значком подсказки или без
 * него. Значок ⓘ — кнопка, и Material отводит ей 48 точек; подпись без
 * значка была высотой в строку текста. Части одной карточки стояли
 * поэтому с разными промежутками: «Язык чека» прижимался к сегментам,
 * а «Макет печати» со значком отходил от них на треть кнопки. Подписи
 * к тому же были набраны разными ролями шкалы — где `titleSmall`, где
 * `bodyMedium`.
 */
@Composable
internal fun PartTitle(title: String, info: String? = null) {
    Row(
        modifier = Modifier.heightIn(min = LocalMinimumInteractiveComponentSize.current),
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f, fill = false)
        )
        info?.let { InfoTip(it) }
    }
}
