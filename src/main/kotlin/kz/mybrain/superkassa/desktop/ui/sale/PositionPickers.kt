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
 *
 * Код, которого в перечне нет, показывается как есть — тем же правилом,
 * каким его показывает строка чека. Пустое поле на такой код кассир
 * читал как «единица не задана», тогда как в чек она уходила.
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
        title = { unit -> unitTitle(units, unit?.code ?: selected) },
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
 *
 * Ставка, которой в перечне узла нет, показывается своим кодом, а не
 * пустотой: в чек эта ставка уходит, и увидеть её кассир обязан до
 * отказа узла, а не после.
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
        title = { rate -> vatTitle(rates, rate?.code ?: selected) },
        onSelect = { onSelect(it.code) },
        modifier = modifier
    )
}
