package kz.mybrain.superkassa.desktop.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.ui.components.InfoTip
import kz.mybrain.superkassa.desktop.ui.sale.SalePanel
import kz.mybrain.superkassa.desktop.ui.sale.SalePanels
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Каким кассир увидит экран продажи.
 *
 * Разделы кассовой колонки сворачиваются и на самом экране, но выбор
 * там живёт до перезапуска. Здесь задаётся, с чего смена начинается:
 * на одном рабочем месте порядок работы один и тот же, и повторять
 * три нажатия каждое утро кассир не должен.
 */
@Composable
internal fun PanelBehaviourCard(session: Session) {
    val texts = LocalStrings.current
    val panels = remember { SalePanels(session.preferences) }
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.roomy),
            verticalArrangement = Arrangement.spacedBy(Spacing.snug)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(texts.settings.panelBehaviour, style = MaterialTheme.typography.titleMedium)
                InfoTip(texts.settings.panelBehaviourHint)
            }
            SalePanel.entries.forEach { panel ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = panels.expanded(panel),
                        onCheckedChange = { panels.toggle(panel) }
                    )
                    Text(panel.title(texts.settings), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
