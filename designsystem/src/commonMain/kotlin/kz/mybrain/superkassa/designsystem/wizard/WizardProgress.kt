package kz.mybrain.superkassa.designsystem.wizard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.theme.size.Spacing

/**
 * Где мастер сейчас: «Шаг 2 из 5» и полоса того же хода.
 *
 * @property number номер шага от единицы.
 * @property count сколько шагов всего.
 * @property label то же словами на языке окна.
 */
class WizardProgress(val number: Int, val count: Int, val label: String)

/** Ход мастера: подпись над полосой, полоса — во всю ширину колонки. */
@Composable
internal fun ProgressLine(progress: WizardProgress, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.itemGap)) {
        Text(
            text = progress.label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LinearProgressIndicator(
            progress = { progress.number.toFloat() / progress.count.coerceAtLeast(1) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
