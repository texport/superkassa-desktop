package kz.mybrain.superkassa.presentation.setup.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.presentation.strings.setup.SetupTexts
import kz.mybrain.superkassa.presentation.theme.StatusColors
import kz.mybrain.superkassa.presentation.theme.icon.AppIcons
import kz.mybrain.superkassa.presentation.theme.size.Sizes
import kz.mybrain.superkassa.presentation.theme.size.Spacing

/**
 * Один шаг подключения кассы.
 *
 * Все шаги видны сразу и всегда: владелец, вернувшийся к брошенному
 * мастеру, должен увидеть пройденное, а не гадать, с чего начинали.
 * Пройденный шаг помечен и свёрнут до итога, недоступный — назван тем,
 * чего ждёт, а не просто погашен.
 *
 * @param done шаг пройден: вместо полей показан его итог.
 * @param ready предыдущие шаги пройдены и этот можно делать сейчас.
 * @param summary итог пройденного шага одной строкой.
 */
@Composable
fun SetupStepCard(
    title: String,
    hint: String,
    texts: SetupTexts,
    done: Boolean,
    ready: Boolean,
    summary: String = "",
    content: @Composable ColumnScope.() -> Unit
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.normal),
            verticalArrangement = Arrangement.spacedBy(Spacing.tight)
        ) {
            StepHeader(title, texts, done, ready)
            Text(
                text = if (done && summary.isNotBlank()) summary else hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (ready || done) {
                content()
            }
        }
    }
}

/** Название шага: пройденный помечен, недоступный назван тем, чего ждёт. */
@Composable
private fun StepHeader(title: String, texts: SetupTexts, done: Boolean, ready: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (done) {
            Icon(
                imageVector = AppIcons.chosen,
                contentDescription = texts.done,
                tint = StatusColors.delivered,
                modifier = Modifier.size(Sizes.infoIcon)
            )
        }
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        if (!ready && !done) {
            Text(
                text = texts.waiting,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
