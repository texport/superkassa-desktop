package kz.mybrain.superkassa.presentation.kassa.sale.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import kz.mybrain.superkassa.designsystem.picker.FieldChoice
import kz.mybrain.superkassa.designsystem.theme.icon.Glyphs
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.domain.kassa.model.Percent
import kz.mybrain.superkassa.domain.kassa.model.Tenge
import kz.mybrain.superkassa.domain.kassa.model.sale.Adjustment
import kz.mybrain.superkassa.domain.kassa.model.sale.AdjustmentUnit
import kz.mybrain.superkassa.presentation.common.format.Money
import kz.mybrain.superkassa.presentation.kassa.field.MoneyField
import kz.mybrain.superkassa.presentation.kassa.sale.LocalSaleTexts
import kz.mybrain.superkassa.presentation.words.kassa.sign
import kz.mybrain.superkassa.strings.api.fill

/**
 * Поле скидки или наценки и способ ввода рядом с ним.
 *
 * Одно на весь экран продажи: скидка на чек, наценка на чек и скидка
 * на позицию — одно и то же понятие, и два разных способа его набрать
 * кассир читать не должен.
 *
 * Способ ввода — у каждого поля свой, в его строке ([FieldChoice]):
 * полей на экране несколько, и общий переключатель менял бы смысл
 * соседнего молча. Под полем написано то же число другим способом —
 * набравший процент видит тенге, набравший тенге видит долю.
 */
@Composable
internal fun AdjustmentField(
    label: String,
    change: Adjustment,
    modifier: Modifier,
    isError: Boolean,
    supportingText: String?,
    onEnter: (String) -> Unit,
    onSwitch: (AdjustmentUnit) -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.fieldGap),
        verticalAlignment = Alignment.Top
    ) {
        MoneyField(
            value = change.text,
            label = label,
            modifier = Modifier.weight(1f),
            isError = isError,
            supportingText = supportingText,
            onValueChange = onEnter
        )
        FieldChoice(AdjustmentUnit.entries, change.unit, { it.sign }, onSwitch)
    }
}

/**
 * То же число другим способом — или `null`, пока показывать нечего.
 *
 * Именно `null`, а не пустая строка: строка под полем занимает место
 * всегда, и ненабранная скидка отодвигала соседнее поле дальше, чем
 * отстоят друг от друга поля соседних карточек.
 *
 * @param base то, от чего берётся процент: сумма позиций у чека
 *   и стоимость строки у позиции.
 * @param asPercent надпись о доле — своя у чека и у позиции: доля
 *   считается от разного, и умолчать об этом значит соврать.
 */
@Composable
internal fun sameOtherwise(change: Adjustment, base: Long?, asPercent: String): String? {
    val extra = LocalSaleTexts.current
    val entered = change.entered
    if (entered == null || base == null) return null
    return when (change.unit) {
        AdjustmentUnit.Percent -> extra.changeAsSum.fill(Money.formatTiyn(Percent.of(base, entered)))
        AdjustmentUnit.Tenge -> Percent.share(base, Tenge.of(entered))?.let { asPercent.fill(formatPercent(it)) }
    }
}

/**
 * Доля в процентах словами экрана.
 *
 * Лишние нули отброшены: «10 %» вместо «10,00 %». Знак держится при числе
 * неразрывным пробелом, как и валюта у суммы.
 */
internal fun formatPercent(value: Decimal): String {
    val written = value.toString()
    val plain = if ('.' in written) written.trimEnd('0').trimEnd('.') else written
    return "${plain.replace('.', Glyphs.DECIMAL)}${Glyphs.NBSP}${Glyphs.PERCENT}"
}
