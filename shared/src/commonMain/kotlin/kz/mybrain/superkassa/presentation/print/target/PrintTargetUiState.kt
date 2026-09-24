package kz.mybrain.superkassa.presentation.print.target

import kz.mybrain.superkassa.domain.print.model.PrintKind
import kz.mybrain.superkassa.domain.print.port.PrintChoices

/**
 * Куда печатает эта касса: принтер, копии и вид файла.
 *
 * @property kkmId касса, чей принтер показан; `null` — касса не выбрана.
 * @property printers принтеры этой машины; прочитаны, если [printersRead].
 * @property printer принтер кассы; `null` — системный по умолчанию.
 */
data class PrintTargetUiState(
    val kkmId: String? = null,
    val printers: List<String> = emptyList(),
    val printersRead: Boolean = false,
    val printer: String? = null,
    val copies: Int = 1,
    val kind: PrintKind = PrintKind.Pdf
) {
    /** Сколько копий можно выбрать. */
    val copyChoices: List<Int> get() = (1..PrintChoices.MAX_COPIES).toList()

    /**
     * Принтеров на машине нет вовсе.
     *
     * За прилавком это обычное дело до подключения чекового; молчание здесь
     * кончалось отказом печати на первом же чеке, при покупателе.
     */
    val noPrinters: Boolean get() = printersRead && printers.isEmpty()

    /**
     * Выбранного принтера в системе больше нет: его отключили или удалили.
     *
     * Печать откажет, а не уйдёт на системный, — и причина видна здесь же,
     * где её исправляют, а не только в отказе при покупателе.
     */
    val printerGone: Boolean get() = printersRead && printers.isNotEmpty() && printer != null && printer !in printers
}
