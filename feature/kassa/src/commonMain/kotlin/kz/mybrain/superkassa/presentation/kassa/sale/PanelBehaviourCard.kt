package kz.mybrain.superkassa.presentation.kassa.sale

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kz.mybrain.superkassa.designsystem.list.ListRows
import kz.mybrain.superkassa.designsystem.picker.SwitchRow
import kz.mybrain.superkassa.designsystem.section.SettingGroup
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.domain.workplace.port.WorkplaceMemory

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
    val panels = remember(memory) { SalePanels(memory) }
    PanelBehaviourGroup(expanded = panels::expanded, onToggle = panels::toggle)
}

/**
 * Разделы кассовой колонки переключателями — по состоянию, без памяти
 * рабочего места: так группу рисуют и превью.
 *
 * Настройка — сворачивание: переключатель включён — раздел при входе
 * в продажу свёрнут, как её и называет владелец («включаю сворачивание»).
 * Когда включённый переключатель значил «развёрнут», владелец включал
 * его, чтобы свернуть, и видел разделы развёрнутыми. Под названием
 * сказано, что в разделе.
 *
 * @param expanded развёрнут ли раздел, когда продажу открывают.
 * @param onToggle раздел переключили.
 */
@Composable
fun PanelBehaviourGroup(expanded: (SalePanel) -> Boolean, onToggle: (SalePanel) -> Unit) {
    val texts = LocalStrings.current
    SettingGroup(
        title = texts.settingsScreen.panelBehaviour,
        info = texts.settingsScreen.panelBehaviourHint
    ) {
        ListRows {
            SalePanel.entries.forEach { panel ->
                val words = panel.setting?.invoke(texts.settingsScreen) ?: return@forEach
                SwitchRow(words.title, checked = !expanded(panel), { onToggle(panel) }, hint = words.hint)
            }
        }
    }
}
