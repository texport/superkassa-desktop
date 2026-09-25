package kz.mybrain.superkassa.presentation.kassa.sale.component

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.list.ScrollableList
import kz.mybrain.superkassa.designsystem.state.EmptyState
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.domain.kassa.model.sale.Basket
import kz.mybrain.superkassa.presentation.kassa.sale.LocalSaleTexts
import kz.mybrain.superkassa.presentation.kassa.sale.position.LocalVatRates
import kz.mybrain.superkassa.presentation.kassa.sale.position.PositionDetailsDialog
import kz.mybrain.superkassa.presentation.kassa.sale.position.PositionRow
import kz.mybrain.superkassa.presentation.kassa.sale.position.details

/**
 * Корзина чека на экране.
 *
 * Лист чека: шапка с числом позиций, под ней сам список. Список
 * ленивый и прокручивается сам — колонка чека больше не прокручивается
 * целиком, и длинный чек не уводит вниз ни итог, ни кнопку.
 *
 * Нажатие на строку открывает её подробности поверх листа. Действия
 * из окна идут теми же обработчиками, что и значки на строке: сторно
 * из окна и сторно со строки — одно и то же сторно.
 */
@Composable
internal fun BasketCard(
    basket: Basket,
    modifier: Modifier = Modifier,
    onStorno: (Int) -> Unit,
    onExcise: (Int) -> Unit,
    onRemove: (Int) -> Unit
) {
    // Открытая строка переживает поворот экрана, как и сама корзина.
    var detailed by rememberSaveable { mutableStateOf<Int?>(null) }
    detailed?.let { at ->
        // Строка читается из корзины на каждом кадре: сторно из окна меняет
        // её, и окно обязано показать уже новое состояние, а не снимок.
        val position = basket.positions.getOrNull(at)
        if (position == null) {
            detailed = null
        } else {
            PositionDetailsDialog(
                details = position.details(),
                rates = LocalVatRates.current,
                onDismiss = { detailed = null },
                onStorno = { onStorno(at) },
                // Удалённой строки в корзине нет, а её место занимает
                // следующая: без закрытия окно показало бы соседку.
                onRemove = {
                    onRemove(at)
                    detailed = null
                }
            )
        }
    }
    Card(modifier = modifier.fillMaxWidth()) {
        BasketSummary(basket)
        HorizontalDivider()
        if (basket.positions.isEmpty()) {
            EmptyBasket(Modifier.weight(1f))
        } else {
            PositionList(basket, Modifier.weight(1f), { detailed = it }, onStorno, onExcise, onRemove)
        }
    }
}

/** Шапка листа: сколько позиций набрано. Сумма стоит в денежном блоке. */
@Composable
private fun BasketSummary(basket: Basket) {
    val extra = LocalSaleTexts.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.cardPadding, vertical = Spacing.fieldGap),
        horizontalArrangement = Arrangement.spacedBy(Spacing.buttonGap),
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
    onOpen: (Int) -> Unit,
    onStorno: (Int) -> Unit,
    onExcise: (Int) -> Unit,
    onRemove: (Int) -> Unit
) {
    ScrollableList(modifier = modifier.fillMaxWidth()) {
        itemsIndexed(basket.positions) { index, position ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            PositionRow(
                position = position,
                onOpen = { onOpen(index) },
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
