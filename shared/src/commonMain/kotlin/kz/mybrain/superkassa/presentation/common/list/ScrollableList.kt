package kz.mybrain.superkassa.presentation.common.list

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import kz.mybrain.superkassa.presentation.common.keyboard.scrolledByKeys
import kz.mybrain.superkassa.presentation.theme.size.Spacing

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
    // Полоса ростом со список, а не с доступное место: список из трёх
    // строк не растягивается пустой рамкой до низа окна. Высоту задаёт
    // сам список — и то, что его раскладка велела занять.
    Layout(
        modifier = modifier,
        content = {
            LazyColumn(
                state = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .scrolledByKeys(state) { state.layoutInfo.viewportSize.height }
                    .padding(end = Spacing.scrollbarGutter),
                content = content
            )
            ListScrollbar(state, Modifier)
        }
    ) { measurables, constraints ->
        val list = measurables[0].measure(constraints)
        val tall = Constraints(maxWidth = constraints.maxWidth, minHeight = list.height, maxHeight = list.height)
        // На Android полосы нет — платформа рисует свою: второго ребёнка может не быть.
        val bar = measurables.getOrNull(1)?.measure(tall)
        layout(list.width, list.height) {
            list.place(0, 0)
            bar?.place(list.width - bar.width, 0)
        }
    }
}
