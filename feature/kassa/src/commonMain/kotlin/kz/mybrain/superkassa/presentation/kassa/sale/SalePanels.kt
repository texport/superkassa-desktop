package kz.mybrain.superkassa.presentation.kassa.sale

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kz.mybrain.superkassa.domain.workplace.port.WorkplaceMemory
import kz.mybrain.superkassa.strings.api.common.SettingStrings

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
 * Экран продажи держит выбор в своей модели; этот держатель остался
 * карточке настроек. Оба пишут в одну память рабочего места, и модель
 * продажи перечитывает выбор, когда экран открывают.
 */
internal class SalePanels(private val memory: WorkplaceMemory) {

    private var collapsed: Set<SalePanel> by mutableStateOf(
        memory.collapsedPanels.mapNotNull { name ->
            SalePanel.entries.firstOrNull { it.name == name }
        }.toSet()
    )

    /** Развёрнут ли раздел сейчас. */
    fun expanded(panel: SalePanel): Boolean = panel !in collapsed

    /** Сворачивает развёрнутый раздел и наоборот; выбор запоминается. */
    fun toggle(panel: SalePanel) {
        collapsed = if (panel in collapsed) collapsed - panel else collapsed + panel
        memory.collapsedPanels = collapsed.map { it.name }.toSet()
    }
}
