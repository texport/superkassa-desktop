package kz.mybrain.superkassa.desktop.ui.sale

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.ui.components.EmptyState
import kz.mybrain.superkassa.desktop.ui.components.ScrollableList
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Корзина чека на экране.
 *
 * Лист чека: шапка с числом позиций и суммой, под ней сам список. Список
 * ленивый и прокручивается сам — колонка чека больше не прокручивается
 * целиком, и длинный чек не уводит вниз ни итог, ни кнопку.
 */
@Composable
fun BasketCard(
    basket: Basket,
    modifier: Modifier = Modifier,
    onStorno: (Int) -> Unit,
    onExcise: (Int) -> Unit,
    onRemove: (Int) -> Unit
) {
    Card(modifier = modifier.fillMaxWidth()) {
        BasketSummary(basket)
        HorizontalDivider()
        if (basket.positions.isEmpty()) {
            EmptyBasket(Modifier.weight(1f))
        } else {
            PositionList(basket, Modifier.weight(1f), onStorno, onExcise, onRemove)
        }
    }
}

/** Шапка листа: сколько позиций набрано и на какую сумму. */
@Composable
private fun BasketSummary(basket: Basket) {
    val extra = LocalSaleTexts.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.normal, vertical = Spacing.snug),
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Сумма позиций стоит в денежном блоке справа и повторять её здесь
        // незачем: два одинаковых числа на одном экране кассир сверяет.
        Text(
            text = "${extra.positionsCount}: ${basket.positions.size}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PositionList(
    basket: Basket,
    modifier: Modifier,
    onStorno: (Int) -> Unit,
    onExcise: (Int) -> Unit,
    onRemove: (Int) -> Unit
) {
    ScrollableList(modifier = modifier.fillMaxWidth()) {
        itemsIndexed(basket.positions) { index, position ->
            PositionCard(
                position = position,
                onStorno = { onStorno(index) },
                onExcise = { onExcise(index) },
                onRemove = { onRemove(index) }
            )
        }
    }
}

/**
 * Пустая корзина — обычное начало чека, а не ошибка.
 *
 * Поэтому здесь значок, строка о том, что тут будет, и подсказка, с чего
 * начать. Ни одного красного пятна: кассир открыл смену, а не ошибся.
 */
@Composable
private fun EmptyBasket(modifier: Modifier) {
    val extra = LocalSaleTexts.current
    EmptyState(
        icon = AppIcons.emptyBasket,
        title = extra.basketEmpty,
        hint = extra.basketEmptyHint,
        modifier = modifier,
        centered = true
    )
}
