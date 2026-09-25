package kz.mybrain.superkassa.presentation.kassa.sale.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.picker.ChoiceSegments
import kz.mybrain.superkassa.designsystem.picker.LabelledPicker
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.domain.kassa.model.VatScope
import kz.mybrain.superkassa.presentation.kassa.sale.LocalSaleTexts
import kz.mybrain.superkassa.presentation.kassa.sale.SaleUiState
import kz.mybrain.superkassa.presentation.kassa.sale.VatActions
import kz.mybrain.superkassa.presentation.kassa.sale.position.vatTitle
import kz.mybrain.superkassa.strings.api.kassa.SaleTexts

/**
 * НДС чека: на весь чек или по позициям — и ставка, если на весь чек.
 *
 * Стоит рядом со скидкой и наценкой и устроен так же: два уровня
 * взаимоисключающие, и экран не даёт выбрать оба. Способ — сегментами
 * на виду: выбранный читается без списка. На весь чек — ставка чека,
 * по умолчанию ставка кассы, а у позиций ставок нет; по позициям —
 * ставка у каждой позиции.
 *
 * Показывается только плательщику НДС: неплательщик налога не выделяет,
 * и выбирать ему нечего.
 */
@Composable
internal fun ReceiptVatFields(sale: SaleUiState, actions: VatActions) {
    val extra = LocalSaleTexts.current
    val scope = sale.form.vat.scopeAt(sale.vatPayer)
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.itemGap)) {
        Text(LocalStrings.current.sale.vat, style = MaterialTheme.typography.labelLarge)
        ChoiceSegments(
            options = VatScope.entries,
            selected = scope,
            label = { it.title(extra) },
            onSelect = actions::scope
        )
    }
    sale.receiptVat?.let { rate -> ReceiptRatePicker(sale, rate, actions) }
    Hint(problem = null, hint = extra.vatScopeHint)
}

/** Ставка на весь чек: из ставок кассы в пределах её режима. */
@Composable
private fun ReceiptRatePicker(sale: SaleUiState, selected: String, actions: VatActions) {
    val rates = sale.vat(LocalLanguage.current, LocalStrings.current.enums)
    LabelledPicker(
        label = LocalSaleTexts.current.receiptVatRate,
        options = rates,
        selected = rates.firstOrNull { it.code == selected },
        title = { rate -> vatTitle(rates, rate?.code ?: selected) },
        onSelect = { actions.rate(it.code) },
        modifier = Modifier.fillMaxWidth()
    )
}

/** Название способа для кассира. */
private fun VatScope.title(texts: SaleTexts): String = when (this) {
    VatScope.Receipt -> texts.vatOnReceipt
    VatScope.Positions -> texts.vatByPositions
}
