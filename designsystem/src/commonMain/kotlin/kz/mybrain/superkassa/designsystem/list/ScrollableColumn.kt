package kz.mybrain.superkassa.designsystem.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.Dp
import kz.mybrain.superkassa.designsystem.keyboard.scrolledByKeys
import kz.mybrain.superkassa.designsystem.theme.size.Spacing

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
 * @param gutter поле под полосу прокрутки внутри столбца. По умолчанию его
 *   нет: столбец идёт до края раздела, как у главной, а полоса вынесена
 *   за край в поле окна ([Spacing.scrollbarOutset]). Своё поле задаётся
 *   только там, где за краем места нет, — внутри карточки.
 * @param focus чем столбцу забрать ввод с клавиатуры: открытый раздел
 *   листается PageDown сразу, без Tab.
 */
@Composable
fun ScrollableColumn(
    modifier: Modifier = Modifier,
    spacing: Dp = Spacing.cardGap,
    gutter: Dp = Spacing.flush,
    focus: FocusRequester? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val scroll = rememberScrollState()
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scroll)
                .then(focus?.let { Modifier.focusRequester(it) } ?: Modifier)
                .scrolledByKeys(scroll) { scroll.viewportSize }
                .padding(end = gutter),
            verticalArrangement = Arrangement.spacedBy(spacing),
            content = content
        )
        ColumnScrollbar(scroll, Modifier.align(Alignment.CenterEnd).fillMaxHeight().besideEdge(gutter))
    }
}

/**
 * Полоса прокрутки за краем содержимого: без своего поля — в поле окна
 * или зазоре панелей, со своим полем — в нём.
 */
fun Modifier.besideEdge(gutter: Dp = Spacing.flush): Modifier =
    if (gutter == Spacing.flush) offset(x = Spacing.scrollbarOutset) else this
