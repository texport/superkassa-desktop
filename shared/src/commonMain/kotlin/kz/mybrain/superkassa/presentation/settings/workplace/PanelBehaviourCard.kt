package kz.mybrain.superkassa.presentation.settings.workplace

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kz.mybrain.superkassa.designsystem.picker.SwitchRow
import kz.mybrain.superkassa.designsystem.section.SectionCard
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.domain.workplace.port.WorkplaceMemory
import kz.mybrain.superkassa.presentation.kassa.sale.SalePanel
import kz.mybrain.superkassa.presentation.kassa.sale.SalePanels
import kz.mybrain.superkassa.presentation.settings.title

/**
 * Каким кассир увидит экран продажи.
 *
 * Разделы кассовой колонки сворачиваются и на самом экране, но выбор
 * там живёт до перезапуска. Здесь задаётся, с чего смена начинается:
 * на одном рабочем месте порядок работы один и тот же, и повторять
 * три нажатия каждое утро кассир не должен.
 */
@Composable
fun PanelBehaviourCard(memory: WorkplaceMemory) {
    val texts = LocalStrings.current
    val panels = remember(memory) { SalePanels(memory) }
    SectionCard(
        title = texts.settings.panelBehaviour,
        info = texts.settings.panelBehaviourHint
    ) {
        SalePanel.entries.forEach { panel ->
            SwitchRow(panel.title(texts.settings), panels.expanded(panel), { panels.toggle(panel) })
        }
    }
}
