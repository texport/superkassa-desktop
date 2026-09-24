package kz.mybrain.superkassa.presentation.cabinet.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.format.Times
import kz.mybrain.superkassa.designsystem.theme.size.Sizes
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.designsystem.tip.InfoTip
import kz.mybrain.superkassa.strings.api.cabinet.CabinetTexts
import kz.mybrain.superkassa.strings.api.cabinet.EdsTexts
import kz.mybrain.superkassa.strings.api.fill
import kotlin.time.Duration

/**
 * Ожидание подписи в NCALayer — с видимым сроком и отменой.
 *
 * Прежде на этом месте стояла занятая кнопка и больше ничего: владелец
 * подписывал в окне NCALayer, приложение три минуты не двигалось, а затем
 * говорило, что NCALayer не запущен. Прервать это было нечем, кроме
 * закрытия приложения.
 *
 * Поэтому здесь три вещи и ни одной лишней: что идёт, сколько осталось
 * и чем прервать. Кнопка отмены встаёт на место кнопки входа — там же,
 * где ожидание и началось.
 *
 * @param left сколько ожидания осталось; ноль — срок вышел, и отказ уже в пути.
 * @param window весь срок ожидания: от него считается доля полоски.
 */
@Composable
fun SignWait(
    left: Duration,
    window: Duration,
    texts: CabinetTexts,
    eds: EdsTexts,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.itemGap)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.buttonGap)
        ) {
            CircularProgressIndicator(modifier = Modifier.size(Sizes.busyCircle))
            Text(texts.signing, style = MaterialTheme.typography.titleMedium)
            InfoTip(texts.hints.signWait)
        }
        if (left > Duration.ZERO) {
            Text(
                text = eds.remaining.fill(Times.countdown(left)),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LinearProgressIndicator(
                progress = { (left / window).toFloat() },
                modifier = Modifier.fillMaxWidth()
            )
        }
        FilledTonalButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text(eds.cancelWait)
        }
    }
}
