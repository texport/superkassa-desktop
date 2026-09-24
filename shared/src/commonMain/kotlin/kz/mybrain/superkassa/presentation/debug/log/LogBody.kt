package kz.mybrain.superkassa.presentation.debug.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.adaptive.windowMargin
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.strings.api.debug.DebugTexts

/**
 * Содержимое журнала: отбор сверху, строки посередине, обещание о тайном внизу.
 *
 * Одно на обе платформы: на компьютере оно стоит в соседнем окне,
 * на Android — поверх кассы.
 */
@Composable
internal fun LogBody(journal: LogUiState, actions: LogActions, texts: DebugTexts) {
    Column(
        modifier = Modifier.fillMaxSize().padding(windowMargin),
        verticalArrangement = Arrangement.spacedBy(Spacing.fieldGap)
    ) {
        LogFilters(texts, journal, actions)
        LogLines(journal.shown, texts, Modifier.weight(1f))
        Text(
            text = texts.secretsHint,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
