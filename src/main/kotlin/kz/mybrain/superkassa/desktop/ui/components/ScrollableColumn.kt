package kz.mybrain.superkassa.desktop.ui.components

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Прокручиваемый столбец с видимой полосой прокрутки.
 *
 * Настольная касса — не телефон: прокрутка пальцем здесь не подразумевается,
 * и без полосы обрезанное содержимое читается как отсутствующее. Кассир
 * не находил список видов оплаты, потому что он уходил за нижний край
 * без единого признака, что там что-то есть.
 *
 * Заведено один раз на всё приложение: полоса, её ширина и отступ под неё
 * должны быть одинаковыми в каждой панели. Клавиатура двигает столбец
 * так же, как колесо, — см. [scrolledByKeys].
 *
 * @param gutter поле под полосу прокрутки. Задаётся снаружи там, где
 *   от него зависит вёрстка соседей: у колонки касс это поле и есть
 *   зазор до разделителя, и с общим значением он получался вдвое шире
 *   отступа от края экрана.
 */
@Composable
fun ScrollableColumn(
    modifier: Modifier = Modifier,
    spacing: Dp = Spacing.normal,
    gutter: Dp = Spacing.normal,
    content: @Composable ColumnScope.() -> Unit
) {
    val scroll = rememberScrollState()
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scroll)
                .scrolledByKeys(scroll) { scroll.viewportSize }
                .padding(end = gutter),
            verticalArrangement = Arrangement.spacedBy(spacing),
            content = content
        )
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(scroll),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
        )
    }
}
