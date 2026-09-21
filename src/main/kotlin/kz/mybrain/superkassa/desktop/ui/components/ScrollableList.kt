package kz.mybrain.superkassa.desktop.ui.components

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Список с видимой полосой прокрутки.
 *
 * Настольная касса — не телефон: прокрутка пальцем здесь не подразумевается,
 * и список, уходящий за нижний край без единого признака продолжения,
 * читается как весь список целиком. Для журнала документов это значит,
 * что часть смены кассир просто не увидит.
 *
 * Заведено один раз на всё приложение вместе с [ScrollableColumn]: полоса,
 * её ширина и отступ под неё одинаковы везде. Клавиатура двигает список
 * так же, как колесо, — см. [scrolledByKeys].
 */
@Composable
fun ScrollableList(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    content: LazyListScope.() -> Unit
) {
    Box(modifier = modifier) {
        LazyColumn(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .scrolledByKeys(state) { state.layoutInfo.viewportSize.height }
                .padding(end = Spacing.normal),
            content = content
        )
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(state),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
        )
    }
}
