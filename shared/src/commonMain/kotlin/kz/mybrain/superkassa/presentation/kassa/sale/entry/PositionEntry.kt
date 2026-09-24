package kz.mybrain.superkassa.presentation.kassa.sale.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.presentation.common.section.CollapsibleSection
import kz.mybrain.superkassa.presentation.kassa.sale.EntryActions
import kz.mybrain.superkassa.presentation.kassa.sale.LocalSaleTexts
import kz.mybrain.superkassa.presentation.kassa.sale.SaleUiState
import kz.mybrain.superkassa.presentation.theme.size.Spacing

/**
 * Ввод позиции: штрихкодом или руками.
 *
 * Оба пути стоят в одной карточке и в этом порядке: сканер — обычный
 * способ, ручной ввод — исключение для товара без кода. Разнести их по
 * разным местам экрана значило бы заставить кассира выбирать способ
 * глазами до того, как он взял товар в руку.
 *
 * Сворачивается только ручной ввод: поле штрихкода остаётся на экране
 * всегда. Сканер вводит код в то поле, что в фокусе, и спрятать его
 * значило бы остановить обычную работу кассы ради экономии высоты.
 *
 * Добавленная позиция очищает оба пути разом. Прежде ненайденный
 * штрихкод оставался в поле вместе с красной строкой «нет такого
 * штрихкода» и после того, как кассир завёл этот товар руками.
 */
@Composable
fun PositionEntryCard(state: SaleUiState, actions: EntryActions, expanded: Boolean, onToggle: () -> Unit) {
    val extra = LocalSaleTexts.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.fieldGap)
        ) {
            CollapsibleSection(
                title = extra.positionEntry,
                expanded = expanded,
                onToggle = onToggle,
                info = extra.barcodeHint,
                always = { BarcodeField(state, actions) }
            ) {
                HorizontalDivider()
                AddPositionForm(state.draft, actions)
            }
        }
    }
}
