package kz.mybrain.superkassa.presentation.analytics.sales

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kz.mybrain.superkassa.designsystem.status.Chip
import kz.mybrain.superkassa.designsystem.theme.StatusColors
import kz.mybrain.superkassa.designsystem.theme.icon.Glyphs
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.domain.analytics.model.SalesView
import kz.mybrain.superkassa.domain.analytics.model.sellingRegisters
import kz.mybrain.superkassa.domain.analytics.model.silentRegisters
import kz.mybrain.superkassa.presentation.common.format.Money
import kz.mybrain.superkassa.strings.api.analytics.AnalyticsSalesTexts

/**
 * Состояние сети касс одной строкой плашек.
 *
 * Отвечает на вопрос проверяющего: сколько касс торговало и не работает
 * ли часть сети без связи. «С продажами» считается по чекам срока —
 * своей ручки о связи у кабинета нет, и называть это число «на связи»
 * значило обещать то, чего сводка не знает, — а «без чеков» берётся
 * от числа касс компании, чтобы касса, не приславшая ни одного
 * документа, не исчезала из счёта вместе со своей строкой.
 *
 * Открытые смены — свойство касс, и число их одно на раздел: считается
 * по кассам, как в учёте, а не берётся из сводки кабинета — та называла
 * третье. У одной кассы смена стоит в шапке её окна, и плашки здесь нет.
 * Доставки документов здесь нет: документы в очереди и без
 * сведений считаются по всем видам чеков и по отчётам, и рядом с «Чеков»
 * продаж их число читалось как противоречие — оно стоит в карточке
 * доставки, с объяснением. Автономные документы остались: работа мимо
 * связи — это состояние сети, и число берётся из той же сводки доставки.
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
internal fun SalesNetworkPlates(
    view: SalesView,
    texts: AnalyticsSalesTexts,
    modifier: Modifier = Modifier,
    register: String? = null
) {
    val silent = silentRegisters(view.summary, view.registers)
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.buttonGap),
        verticalArrangement = Arrangement.spacedBy(Spacing.inline)
    ) {
        if (register == null) {
            val selling = sellingRegisters(view.registers)
            Plate(selling, texts.online, good(selling))
            Plate(silent, texts.silent, MaterialTheme.colorScheme.onSurfaceVariant)
            view.openShifts?.let { Plate(it, texts.openShifts, MaterialTheme.colorScheme.onSurface) }
        }
        Plate(view.delivery.offline, texts.offline, attention(view.delivery.offline))
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
