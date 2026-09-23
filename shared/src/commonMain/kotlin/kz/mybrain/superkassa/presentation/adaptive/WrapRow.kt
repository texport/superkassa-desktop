package kz.mybrain.superkassa.presentation.adaptive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import kz.mybrain.superkassa.presentation.theme.Spacing

/**
 * Ряд плашек, кнопок и полей, который переносится, а не сжимается.
 *
 * Обычный `Row` отдаёт последнему элементу то, что осталось, и в узком
 * окне плашка фильтра ставила слово столбиком по букве, а кнопка
 * «Обновить» уезжала за край. Здесь элемент, которому не хватает места
 * в строке, уходит на следующую целиком.
 *
 * Поле, которое должно занять остаток строки, получает
 * `Modifier.weight(1f).widthIn(min = …)`: пока его наименьшая ширина
 * помещается, оно тянется до конца строки, а нет — уходит вниз.
 *
 * @param spacing зазор между элементами и между строками.
 */
@Composable
fun WrapRow(
    modifier: Modifier = Modifier,
    spacing: Dp = Spacing.tight,
    content: @Composable FlowRowScope.() -> Unit
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalArrangement = Arrangement.spacedBy(spacing),
        itemVerticalAlignment = Alignment.CenterVertically,
        content = content
    )
}
