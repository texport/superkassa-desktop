package kz.mybrain.superkassa.desktop.ui.sale

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.server.UnitOfMeasurement
import kz.mybrain.superkassa.desktop.ui.components.LabelledPicker
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings

/**
 * Единица измерения позиции.
 *
 * Перечень целиком от узла — справочник ИС ЭСФ. Пока он не прочитан,
 * поля нет вовсе: пустой выбор хуже отсутствующего, а узел без единицы
 * ставит штуку.
 */
@Composable
internal fun UnitPicker(
    selected: String?,
    units: List<UnitOfMeasurement>,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit
) {
    if (units.isEmpty()) return
    val texts = LocalStrings.current
    LabelledPicker(
        label = texts.sale.measureUnit,
        options = units,
        selected = units.firstOrNull { it.code == selected },
        title = { it?.title ?: "" },
        onSelect = { onSelect(it.code) },
        modifier = modifier
    )
}

/**
 * Ставка НДС позиции.
 *
 * Поля нет, когда выбирать не из чего: у кассы-неплательщика перечень
 * сведён к «Без НДС», и список из одной строки только занимает место
 * в кассовой колонке и обещает выбор, которого нет.
 */
@Composable
internal fun VatPicker(selected: String, modifier: Modifier = Modifier, onSelect: (String) -> Unit) {
    val texts = LocalStrings.current
    val rates = LocalVatRates.current
    if (rates.size < 2) return
    LabelledPicker(
        label = texts.sale.vat,
        options = rates,
        selected = rates.firstOrNull { it.code == selected },
        title = { rate -> rate?.let { vatTitle(rates, it.code) }.orEmpty() },
        onSelect = { onSelect(it.code) },
        modifier = modifier
    )
}
