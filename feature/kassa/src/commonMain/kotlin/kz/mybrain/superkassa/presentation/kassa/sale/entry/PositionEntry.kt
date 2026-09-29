package kz.mybrain.superkassa.presentation.kassa.sale.entry

import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.designsystem.section.CollapsibleSection
import kz.mybrain.superkassa.presentation.kassa.sale.EntryActions
import kz.mybrain.superkassa.presentation.kassa.sale.LocalSaleTexts
import kz.mybrain.superkassa.presentation.kassa.sale.SaleUiState
import kz.mybrain.superkassa.presentation.kassa.sale.component.TillCard

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
internal fun PositionEntryCard(state: SaleUiState, actions: EntryActions, expanded: Boolean, onToggle: () -> Unit) {
    val extra = LocalSaleTexts.current
    TillCard {
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
