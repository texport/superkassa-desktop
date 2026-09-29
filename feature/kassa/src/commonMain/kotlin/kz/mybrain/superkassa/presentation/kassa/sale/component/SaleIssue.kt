package kz.mybrain.superkassa.presentation.kassa.sale.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.button.BusyButton
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.designsystem.theme.size.KassaLayout
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.domain.kassa.model.sale.DomainField
import kz.mybrain.superkassa.domain.kassa.model.sale.SaleBlock
import kz.mybrain.superkassa.presentation.kassa.sale.LocalSaleTexts
import kz.mybrain.superkassa.presentation.kassa.sale.SaleUiState
import kz.mybrain.superkassa.presentation.words.kassa.action
import kz.mybrain.superkassa.presentation.words.kassa.reason
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Единственное главное действие экрана и причина, по которой оно недоступно.
 *
 * Кнопка залитая и во всю ширину кассовой колонки: на экране ровно одно
 * действие, ради которого кассир сюда пришёл, и промахнуться по нему нельзя.
 * Причина написана под кнопкой всегда: серая кнопка без объяснения — самая
 * дорогая ошибка кассового интерфейса, кассир не знает, что исправлять.
 */
@Composable
internal fun IssueRow(state: SaleUiState, onIssue: () -> Unit) {
    val block = state.block
    val texts = LocalStrings.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.inline)
    ) {
        BusyButton(
            text = if (state.issuing) texts.receipt.issuing else state.form.operation.action(texts.receipt),
            busy = state.issuing,
            enabled = block == null,
            // Цель нажатия кассы — выше обычной кнопки: её жмут не глядя, под очередью.
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
    val payment = textsOf(LocalLanguage.current).kassa.payment
    Text(
        text = block?.reason(LocalSaleTexts.current, payment, missingField).orEmpty(),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error
    )
}

/**
 * Блок итога: сумма к оплате и «Пробить чек».
 *
 * Выделен контейнером другого тона: это место, ради которого кассир
 * пришёл на экран, и на одной плашке с вводом позиции и скидками оно
 * терялось. Главное число — «К оплате» — самым крупным начертанием
 * денег, кнопка — единственная залитая на экране.
 */
@Composable
internal fun CheckoutPanel(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.fieldGap),
            content = content
        )
    }
}
