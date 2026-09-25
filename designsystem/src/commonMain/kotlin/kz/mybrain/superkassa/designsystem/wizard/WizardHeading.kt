package kz.mybrain.superkassa.designsystem.wizard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import kz.mybrain.superkassa.designsystem.theme.size.Sizes
import kz.mybrain.superkassa.designsystem.theme.size.Spacing

/**
 * Что это за шаг: иллюстрация, название и объяснение.
 *
 * @property explain что происходит на шаге и зачем — два-три предложения.
 */
class WizardHeading(val icon: ImageVector, val title: String, val explain: String)

/**
 * Иллюстрация, заголовок и объяснение шага мастера.
 *
 * Иллюстрация — значок шага в тоновом круге, как крупные значки Material 3
 * в пустых состояниях и диалогах: цвет берётся из роли `secondaryContainer`,
 * своей палитры у картинки нет, и в тёмной теме она темнеет вместе с окном.
 * Заголовок — шкалой `headline` и помечен заголовком для чтения с экрана:
 * по нему шаг узнают с одного взгляда. Объяснение — шкалой `bodyLarge`
 * в приглушённом цвете: его читают один раз, а не каждый приход.
 */
@Composable
internal fun WizardHeader(step: WizardHeading, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.fieldGap)) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
            Icon(
                imageVector = step.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(Spacing.blockPadding).size(Sizes.wizardIcon)
            )
        }
        Text(
            text = step.title,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            text = step.explain,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
