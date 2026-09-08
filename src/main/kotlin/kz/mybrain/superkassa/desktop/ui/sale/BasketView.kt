package kz.mybrain.superkassa.desktop.ui.sale

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import kz.mybrain.superkassa.desktop.ui.components.Money
import kz.mybrain.superkassa.desktop.ui.components.ScrollableList
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.theme.MoneyStyle
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import java.math.BigDecimal

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
    onRemove: (Int) -> Unit
) {
    Card(modifier = modifier.fillMaxWidth()) {
        BasketSummary(basket)
        HorizontalDivider()
        if (basket.positions.isEmpty()) {
            EmptyBasket(Modifier.weight(1f))
        } else {
            PositionList(basket, Modifier.weight(1f), onStorno, onRemove)
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
    onRemove: (Int) -> Unit
) {
    ScrollableList(modifier = modifier.fillMaxWidth()) {
        itemsIndexed(basket.positions) { index, position ->
            PositionCard(
                position = position,
                onStorno = { onStorno(index) },
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
    Column(
        modifier = modifier.fillMaxWidth().padding(Spacing.roomy),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.snug, Alignment.CenterVertically)
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHighest
        ) {
            Icon(
                imageVector = Icons.Outlined.ShoppingCart,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(Spacing.normal)
            )
        }
        Text(extra.basketEmpty, style = MaterialTheme.typography.titleMedium)
        Text(
            text = extra.basketEmptyHint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Строка корзины.
 *
 * Сторно окрашено ролью ошибки целиком, а не помечено одним словом: строка,
 * уводящая итог вниз, обязана отличаться от продажи с одного взгляда.
 */
@Composable
private fun PositionCard(position: Position, onStorno: () -> Unit, onRemove: () -> Unit) {
    val texts = LocalStrings.current
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        ListItem(
            colors = positionColors(position.storno),
            headlineContent = {
                Text(
                    text = position.label(texts.sale.storno),
                    style = MaterialTheme.typography.titleMedium
                )
            },
            supportingContent = {
                Text(positionDetail(position), style = MaterialTheme.typography.bodyMedium)
            },
            trailingContent = { PositionSum(position, onStorno, onRemove) }
        )
    }
}

/** Цвета строки: обычная берёт роли поверхности, сторно — роли ошибки. */
@Composable
private fun positionColors(storno: Boolean) = if (storno) {
    ListItemDefaults.colors(
        containerColor = MaterialTheme.colorScheme.errorContainer,
        headlineColor = MaterialTheme.colorScheme.onErrorContainer,
        supportingColor = MaterialTheme.colorScheme.onErrorContainer,
        trailingIconColor = MaterialTheme.colorScheme.onErrorContainer
    )
} else {
    ListItemDefaults.colors()
}

/** Сумма строки моноширинным столбцом и удаление позиции. */
@Composable
private fun PositionSum(position: Position, onStorno: () -> Unit, onRemove: () -> Unit) {
    val texts = LocalStrings.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatSigned(position.total),
            style = MoneyStyle.row,
            modifier = Modifier.width(Sizes.fieldAmount)
        )
        // Сторно отменяет уже пробитую строку и остаётся в чеке; удаление
        // убирает строку до пробития. Сторнированную сторнировать нечем.
        IconButton(enabled = !position.storno, onClick = onStorno) {
            Icon(Icons.Outlined.Undo, contentDescription = texts.sale.storno)
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Outlined.Delete, contentDescription = texts.sale.remove)
        }
    }
}

/** Из чего сложилась строка: количество, цена за единицу, ставка и скидка. */
@Composable
private fun positionDetail(position: Position): String {
    val texts = LocalStrings.current
    val extra = LocalSaleTexts.current
    val head = "${position.quantity.toPlainString()} × ${Money.format(position.price)}" +
        " · ${vatTitle(LocalVatRates.current, position.vatGroup)}"
    if (position.discount <= BigDecimal.ZERO) return head
    return "$head · ${extra.lineDiscount} ${Money.format(position.discount)}"
}
