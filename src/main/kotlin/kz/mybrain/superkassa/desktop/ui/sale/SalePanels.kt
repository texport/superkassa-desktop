package kz.mybrain.superkassa.desktop.ui.sale

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kz.mybrain.superkassa.desktop.app.Preferences
import kz.mybrain.superkassa.desktop.ui.strings.SettingStrings

/**
 * Разделы кассовой колонки, которые сворачиваются.
 *
 * Названы по смыслу, а не по месту на экране: имя уходит в настройки
 * рабочего места и должно пережить перестановку карточек.
 */
enum class SalePanel(val title: (SettingStrings) -> String) {
    PositionEntry({ it.panelPositionEntry }),
    ReceiptChanges({ it.panelReceiptChanges }),
    CustomerData({ it.panelCustomerData }),
    Money({ it.panelMoney })
}

/**
 * Что развёрнуто в кассовой колонке.
 *
 * Высота колонки одна на три раздела, и кассир распоряжается ею сам:
 * набирает товар — сворачивает деньги и реквизиты, рассчитывается —
 * разворачивает обратно. Выбор запоминается между запусками: порядок
 * работы на рабочем месте один и тот же, и повторять три нажатия каждое
 * утро кассир не должен.
 *
 * Состояние живёт рядом с экраном, а не в [SaleForm]: в чек оно не уходит.
 */
class SalePanels(private val preferences: Preferences) {

    private var collapsed: Set<SalePanel> by mutableStateOf(
        preferences.collapsedPanels.mapNotNull { name ->
            SalePanel.entries.firstOrNull { it.name == name }
        }.toSet()
    )

    /** Развёрнут ли раздел сейчас. */
    fun expanded(panel: SalePanel): Boolean = panel !in collapsed

    /** Сворачивает развёрнутый раздел и наоборот; выбор запоминается. */
    fun toggle(panel: SalePanel) {
        collapsed = if (panel in collapsed) collapsed - panel else collapsed + panel
        preferences.collapsedPanels = collapsed.map { it.name }.toSet()
    }
}
