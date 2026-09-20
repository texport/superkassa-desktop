package kz.mybrain.superkassa.desktop.ui.returns

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import kz.mybrain.superkassa.desktop.server.SoldItem
import kz.mybrain.superkassa.desktop.ui.components.Money
import kz.mybrain.superkassa.desktop.ui.strings.ReturnJournalTexts
import kz.mybrain.superkassa.desktop.ui.theme.MoneyStyle
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import java.math.BigDecimal

/**
 * Позиции чека-основания: что именно возвращают.
 *
 * Возврат «на сумму» годится для чека из одной строки, а покупатель
 * возвращает товар: одну пачку из трёх. Отмеченные позиции уходят
 * в ОФД теми же строками, какими были проданы, — с тем же наименованием,
 * ценой и ставкой.
 *
 * Сторнированные строки не показываются: их в чеке уже нет.
 *
 * @param items состав чека-основания, как его отдал узел.
 * @param chosen отмеченные позиции.
 */
@Composable
internal fun RefundItems(
    items: List<SoldItem>,
    chosen: Set<Int>,
    journal: ReturnJournalTexts,
    onToggle: (Int) -> Unit
) {
    if (items.isEmpty()) return
    Text(
        text = journal.itemsToReturn,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.hairline)) {
        items.forEachIndexed { at, item ->
            if (item.isStorno) return@forEachIndexed
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.tight)
            ) {
                Checkbox(checked = at in chosen, onCheckedChange = { onToggle(at) })
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = Money.format(item.sum),
                    style = MoneyStyle.row
                )
            }
        }
    }
}

/** Сумма отмеченных позиций в тиынах: столько и вернётся покупателю. */
internal fun chosenTiyn(items: List<SoldItem>, chosen: Set<Int>): Long =
    chosen.mapNotNull { items.getOrNull(it) }
        .fold(BigDecimal.ZERO) { sum, item -> sum + item.sum }
        .movePointRight(Money.TIYN_SCALE)
        .toLong()
