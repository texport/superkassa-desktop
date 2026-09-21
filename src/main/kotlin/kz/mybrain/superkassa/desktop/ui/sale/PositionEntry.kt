package kz.mybrain.superkassa.desktop.ui.sale

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.ui.components.CollapsibleSection
import kz.mybrain.superkassa.desktop.ui.components.InfoTip
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

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
fun PositionEntryCard(
    session: Session,
    expanded: Boolean,
    onToggle: () -> Unit,
    onAdd: (Position) -> Unit
) {
    val extra = LocalSaleTexts.current
    var added by remember { mutableStateOf(0) }
    val add: (Position) -> Unit = {
        added += 1
        onAdd(it)
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.normal),
            verticalArrangement = Arrangement.spacedBy(Spacing.snug)
        ) {
            CollapsibleSection(
                title = extra.positionEntry,
                expanded = expanded,
                onToggle = onToggle,
                trailing = { InfoTip(extra.barcodeHint) },
                always = { BarcodeField(session, added, add) }
            ) {
                HorizontalDivider()
                AddPositionForm(session, add)
            }
        }
    }
}
