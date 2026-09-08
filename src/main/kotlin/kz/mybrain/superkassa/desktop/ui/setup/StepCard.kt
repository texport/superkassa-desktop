package kz.mybrain.superkassa.desktop.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.ui.components.InfoTip
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Шаг: подпись над карточкой, содержимое внутри.
 *
 * Подпись вынесена наружу намеренно: так видно, что шагов два и какой
 * из них перед глазами, — а не сплошная форма с заголовками внутри.
 */
@Composable
fun Step(title: String, explanation: String, content: @Composable ColumnScope.() -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        InfoTip(explanation)
    }
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.roomy),
            verticalArrangement = Arrangement.spacedBy(Spacing.snug),
            content = content
        )
    }
}

/**
 * Короткое правило под полями шага.
 *
 * Только то, без чего шаг не пройти, и одной строкой: длинное объяснение
 * живёт в подсказке у заголовка шага.
 */
@Composable
fun StepHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
