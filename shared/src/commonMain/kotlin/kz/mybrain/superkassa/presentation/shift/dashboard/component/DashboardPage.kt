package kz.mybrain.superkassa.presentation.shift.dashboard.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.max
import kz.mybrain.superkassa.designsystem.theme.size.KassaLayout
import kz.mybrain.superkassa.designsystem.theme.size.Sizes
import kz.mybrain.superkassa.designsystem.theme.size.Spacing

/**
 * Страница главного экрана: плитки и кнопки смены сверху, списки смены —
 * всё, что осталось.
 *
 * Пока спискам остаётся не меньше [KassaLayout.dashboardListsMin], страница
 * стоит, как стояла. На невысоком окне с крупным шрифтом и строкой о
 * пределе суток верх съедал почти всю высоту: карточке отказов доставалась
 * полоска в восемь точек, а списку документов — ноль. Там спискам отдаётся
 * их наименьшая высота, и страница прокручивается целиком.
 *
 * @param top плитки, карточки и кнопки над списками.
 * @param lists списки смены; получают свою высоту готовым модификатором.
 */
@Composable
internal fun DashboardPage(top: @Composable ColumnScope.() -> Unit, lists: @Composable (Modifier) -> Unit) {
    val density = LocalDensity.current
    var topHeight by remember { mutableStateOf(Sizes.unmeasured) }
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val rest: Dp = maxHeight - Spacing.cardGap - topHeight
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.cardGap)
        ) {
            Column(
                modifier = Modifier.onSizeChanged { topHeight = with(density) { it.height.toDp() } },
                verticalArrangement = Arrangement.spacedBy(Spacing.cardGap),
                content = top
            )
            lists(Modifier.fillMaxWidth().height(max(rest, KassaLayout.dashboardListsMin)))
        }
    }
}
