package kz.mybrain.superkassa.presentation.kassa.refund.component

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
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptItemView
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.designsystem.theme.type.MoneyStyle
import kz.mybrain.superkassa.domain.kassa.model.refund.RefundDraft
import kz.mybrain.superkassa.presentation.common.format.Money
import kz.mybrain.superkassa.presentation.kassa.refund.ReturnsUiState
import kz.mybrain.superkassa.presentation.kassa.sale.position.PositionDetailsDialog
import kz.mybrain.superkassa.presentation.kassa.sale.position.details
import kz.mybrain.superkassa.presentation.kassa.sale.position.vatRatesOf
import kz.mybrain.superkassa.strings.api.journal.ReturnJournalTexts

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
 */
@Composable
internal fun RefundItems(
    state: ReturnsUiState,
    draft: RefundDraft,
    journal: ReturnJournalTexts,
    onToggle: (Int) -> Unit
) {
    val items = draft.items
    if (items.isEmpty()) return
    var detailed by remember(items) { mutableStateOf<Int?>(null) }
    detailed?.let { at -> SoldDetails(state, items[at]) { detailed = null } }
    Text(
        text = journal.itemsToReturn,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.inline)) {
        items.forEachIndexed { at, item ->
            if (!item.isStorno) {
                SoldRow(item.name, at in draft.chosen, draft.shares[at], onOpen = { detailed = at }) { onToggle(at) }
            }
        }
    }
}

/**
 * Строка чека-основания: флажок, имя и доля итога.
 *
 * Строка стоит своей доли итога чека: при скидке на весь чек она меньше
 * суммы строки, и вернётся покупателю именно она.
 */
@Composable
private fun SoldRow(name: String, chosen: Boolean, share: Long, onOpen: () -> Unit, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.buttonGap)
    ) {
        Checkbox(checked = chosen, onCheckedChange = { onToggle() })
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(text = Money.formatTiyn(share), style = MoneyStyle.row)
    }
}

/**
 * Подробности проданной строки без действий: пробитый чек не правят.
 *
 * Ставки берутся те же, что видит экран продажи: у неизвестного кода
 * окно покажет сам код, а не пустое место.
 */
@Composable
private fun SoldDetails(state: ReturnsUiState, item: ReceiptItemView, onDismiss: () -> Unit) {
    val language = LocalLanguage.current
    PositionDetailsDialog(
        details = item.details(),
        rates = vatRatesOf(state.vatRates, state.kkm, language, LocalStrings.current.enums),
        onDismiss = onDismiss
    )
}
