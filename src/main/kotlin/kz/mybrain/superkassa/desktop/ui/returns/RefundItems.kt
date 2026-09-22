package kz.mybrain.superkassa.desktop.ui.returns

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.SoldItem
import kz.mybrain.superkassa.desktop.ui.components.Money
import kz.mybrain.superkassa.desktop.ui.sale.PositionDetailsDialog
import kz.mybrain.superkassa.desktop.ui.sale.details
import kz.mybrain.superkassa.desktop.ui.sale.vatRatesOf
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.ReturnJournalTexts
import kz.mybrain.superkassa.desktop.ui.strings.saleTexts
import kz.mybrain.superkassa.desktop.ui.theme.MoneyStyle
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

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
 * Нажатие на строку мимо флажка открывает её подробности: в строке
 * только имя и сумма, а покупатель у прилавка спорит о цене и весе.
 *
 * @param items состав чека-основания, как его отдал узел.
 * @param chosen отмеченные позиции.
 */
@Composable
internal fun RefundItems(
    session: Session,
    items: List<SoldItem>,
    chosen: Set<Int>,
    journal: ReturnJournalTexts,
    onToggle: (Int) -> Unit
) {
    if (items.isEmpty()) return
    var detailed by remember(items) { mutableStateOf<Int?>(null) }
    detailed?.let { at -> SoldDetails(session, items[at]) { detailed = null } }
    Text(
        text = journal.itemsToReturn,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.hairline)) {
        items.forEachIndexed { at, item ->
            if (item.isStorno) return@forEachIndexed
            Row(
                modifier = Modifier.fillMaxWidth().clickable { detailed = at },
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

/**
 * Подробности проданной строки без действий: пробитый чек не правят.
 *
 * Ставки берутся те же, что видит экран продажи: у неизвестного кода
 * окно покажет сам код, а не пустое место.
 */
@Composable
private fun SoldDetails(session: Session, item: SoldItem, onDismiss: () -> Unit) {
    PositionDetailsDialog(
        details = item.details(),
        texts = saleTexts(session.language),
        units = session.units,
        rates = vatRatesOf(session, LocalStrings.current.enums),
        onDismiss = onDismiss
    )
}

/** Сумма отмеченных позиций в тиынах: столько и вернётся покупателю. */
internal fun chosenTiyn(items: List<SoldItem>, chosen: Set<Int>): Long =
    itemsTiyn(chosen.mapNotNull { items.getOrNull(it) })
