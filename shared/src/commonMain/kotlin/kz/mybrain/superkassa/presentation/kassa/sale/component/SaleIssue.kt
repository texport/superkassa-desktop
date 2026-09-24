package kz.mybrain.superkassa.presentation.kassa.sale.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.domain.kassa.model.sale.DomainField
import kz.mybrain.superkassa.domain.kassa.model.sale.SaleBlock
import kz.mybrain.superkassa.presentation.common.button.BusyButton
import kz.mybrain.superkassa.presentation.kassa.sale.LocalSaleTexts
import kz.mybrain.superkassa.presentation.kassa.sale.SaleUiState
import kz.mybrain.superkassa.presentation.strings.common.LocalLanguage
import kz.mybrain.superkassa.presentation.strings.common.LocalStrings
import kz.mybrain.superkassa.presentation.strings.kassa.action
import kz.mybrain.superkassa.presentation.strings.kassa.paymentTexts
import kz.mybrain.superkassa.presentation.strings.kassa.reason
import kz.mybrain.superkassa.presentation.theme.size.KassaLayout
import kz.mybrain.superkassa.presentation.theme.size.Spacing

/**
 * Единственное главное действие экрана и причина, по которой оно недоступно.
 *
 * Кнопка залитая и во всю ширину кассовой колонки: на экране ровно одно
 * действие, ради которого кассир сюда пришёл, и промахнуться по нему нельзя.
 * Причина написана под кнопкой всегда: серая кнопка без объяснения — самая
 * дорогая ошибка кассового интерфейса, кассир не знает, что исправлять.
 */
@Composable
fun IssueRow(state: SaleUiState, onIssue: () -> Unit) {
    val block = state.block
    val texts = LocalStrings.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.hairline)
    ) {
        BusyButton(
            text = if (state.issuing) texts.sale.issuing else state.form.operation.action(texts.sale),
            busy = state.issuing,
            enabled = block == null,
            modifier = Modifier.fillMaxWidth().heightIn(min = KassaLayout.mainAction),
            onClick = onIssue
        )
        BlockReason(block, state.form.domain.missing(state.domainKind))
    }
}

/** Почему пробить нельзя. Место под строкой занято всегда: иначе кнопка прыгает. */
@Composable
private fun BlockReason(block: SaleBlock?, missingField: DomainField?) {
    // Одна причина без вступления: кнопка рядом и так погашена.
    Text(
        text = block?.reason(LocalSaleTexts.current, paymentTexts(LocalLanguage.current), missingField).orEmpty(),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error
    )
}
