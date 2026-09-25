package kz.mybrain.superkassa.designsystem.list

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import kz.mybrain.superkassa.designsystem.keyboard.scrolledByKeys

/**
 * Список с видимой полосой прокрутки.
 *
 * Настольная касса — не телефон: прокрутка пальцем здесь не подразумевается,
 * и список, уходящий за нижний край без единого признака продолжения,
 * читается как весь список целиком. Для журнала документов это значит,
 * что часть смены кассир просто не увидит.
 *
 * Заведено один раз на всё приложение вместе с [ScrollableColumn]: полоса
 * и её ширина одинаковы везде. Клавиатура двигает список так же, как
 * колесо, — см. [scrolledByKeys].
 *
 * Строки идут во всю ширину, а полоса ложится поверх их края: у строки
 * списка своё поле до содержимого шире полосы. Поле под полосу справа
 * обрезало фон выбранной строки, и справа от неё стояла пустая полоска —
 * а на Android и iOS своей полосы нет вовсе, и поле пустовало всегда.
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
                    .scrolledByKeys(state) { state.layoutInfo.viewportSize.height },
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
