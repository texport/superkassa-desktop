package kz.mybrain.superkassa.desktop.ui.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kz.mybrain.superkassa.desktop.ui.components.Chip
import kz.mybrain.superkassa.desktop.ui.components.Money
import kz.mybrain.superkassa.desktop.ui.strings.AnalyticsSalesTexts
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import kz.mybrain.superkassa.desktop.ui.theme.StatusColors

/**
 * Состояние сети касс одной строкой плашек.
 *
 * Отвечает на вопрос проверяющего: все ли кассы на связи и не работает
 * ли часть сети мимо БФД. «На связи» считается по чекам срока — своей
 * ручки о связи у кабинета нет, — а «молчат» берётся от числа касс
 * компании, чтобы касса, не приславшая ни одного документа, не исчезала
 * из счёта вместе со своей строкой.
 *
 * Цветом красится только то, что требует работы, и только то, о чём
 * сводка знает наверняка. Касс без чеков за срок цвет не касается:
 * сводка не отличает потерянную связь от кассы, которой КГД ещё не дал
 * учёта, — а в парке показа из 3294 касс таких 3288, и красное число
 * посылало владельца искать поломку, которой нет. Плашки о заблокированных
 * кассах здесь нет вовсе — сводка кабинета о блокировках не отвечает,
 * и выдумывать это число нельзя.
 *
 * @param register касса, которой ограничен отбор; `null` — вся сеть.
 *   В окне одной кассы плашек о числе касс нет: на вопрос о сети внутри
 *   окна про одну машину не отвечают.
 */
@Composable
fun SalesNetworkPlates(
    view: SalesView,
    texts: AnalyticsSalesTexts,
    modifier: Modifier = Modifier,
    register: String? = null
) {
    val silent = silentRegisters(view.summary, view.registers)
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
        verticalArrangement = Arrangement.spacedBy(Spacing.hairline)
    ) {
        if (register == null) {
            val selling = sellingRegisters(view.registers)
            Plate(selling, texts.online, good(selling))
            Plate(silent, texts.silent, MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Plate(view.summary.openShiftCount, texts.openShifts, MaterialTheme.colorScheme.onSurface)
        Plate(view.summary.offlineCount, texts.offline, attention(view.summary.offlineCount))
        Plate(view.summary.queuedCount, texts.queuedCount, waiting(view.summary.queuedCount))
        Plate(view.summary.unknownCount, texts.unknownCount, attention(view.summary.unknownCount))
    }
}

/** Одна плашка сети: число и то, чего оно касается. */
@Composable
private fun Plate(count: Int, label: String, tone: Color) {
    Chip(text = "${Money.count(count)}${Glyphs.SEPARATOR}$label", color = tone)
}

/** Цвет доброй вести; ноль касс на связи вестью не является. */
@Composable
private fun good(count: Int): Color =
    if (count > 0) StatusColors.delivered else MaterialTheme.colorScheme.onSurfaceVariant

/** Цвет числа, которое требует работы; ноль такого не требует. */
@Composable
private fun attention(count: Int): Color =
    if (count > 0) StatusColors.refused else MaterialTheme.colorScheme.onSurfaceVariant

/** То же для того, что доедет само: очередь — это ожидание, а не отказ. */
@Composable
private fun waiting(count: Int): Color =
    if (count > 0) StatusColors.pending else MaterialTheme.colorScheme.onSurfaceVariant
