package kz.mybrain.superkassa.desktop.ui.sale

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.app.titleOf
import kz.mybrain.superkassa.desktop.server.Dictionary
import kz.mybrain.superkassa.desktop.ui.components.BusyButton
import kz.mybrain.superkassa.desktop.ui.payment.unsupportedPayments
import kz.mybrain.superkassa.desktop.ui.strings.AppStrings
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.SaleTexts
import kz.mybrain.superkassa.desktop.ui.strings.paymentTexts
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import java.math.BigDecimal

/**
 * Единственное главное действие экрана и причина, по которой оно недоступно.
 *
 * Кнопка залитая и во всю ширину кассовой колонки: на экране ровно одно
 * действие, ради которого кассир сюда пришёл, и промахнуться по нему нельзя.
 * Причина написана под кнопкой всегда: серая кнопка без объяснения — самая
 * дорогая ошибка кассового интерфейса, кассир не знает, что исправлять.
 */
@Composable
fun IssueRow(session: Session, basket: Basket, form: SaleForm) {
    val block = blockOf(saleStateOf(session, basket, form))
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.hairline)
    ) {
        IssueButton(session, basket, form, enabled = block == null)
        BlockReason(session, block, form.domain.missing)
    }
}

@Composable
private fun IssueButton(session: Session, basket: Basket, form: SaleForm, enabled: Boolean) {
    val texts = LocalStrings.current
    val extra = LocalSaleTexts.current
    val scope = rememberCoroutineScope()
    BusyButton(
        text = if (form.issuing) texts.sale.issuing else form.operation.action(texts.sale),
        busy = form.issuing,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        onClick = { scope.launch { issue(session, basket, form, texts, extra) } }
    )
}

/** Почему пробить нельзя. Место под строкой занято всегда: иначе кнопка прыгает. */
@Composable
private fun BlockReason(session: Session, block: SaleBlock?, missingField: DomainField?) {
    val texts = LocalStrings.current
    val extra = LocalSaleTexts.current
    // Одна причина без вступления: кнопка рядом и так погашена, а «Чек
    // пробить нельзя: В чеке нет ни одной позиции» — два раза об одном
    // и с заглавной буквы посреди фразы.
    Text(
        text = block?.reason(texts.sale, extra, paymentTexts(session.language), missingField).orEmpty(),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error
    )
}

/** Оформление чека: пока идёт обращение к узлу, кнопка занята. */
private suspend fun issue(
    session: Session,
    basket: Basket,
    form: SaleForm,
    texts: AppStrings,
    extra: SaleTexts
) {
    form.issuing = true
    val total = basket.totalWith(amount(form.discount).value, amount(form.markup).value)
    val issued = issueReceipt(session, basket, form.input(total), texts, extra) {
        session.titleOf(Dictionary.DeliveryStatuses, it)
    }
    if (issued) form.startNextReceipt()
    form.issuing = false
}

/** Снимок состояния экрана для правил: чистые данные, без Compose. */
fun saleStateOf(session: Session, basket: Basket, form: SaleForm): SaleState = saleStateOf(
    session,
    basket,
    form,
    basket.totalWith(amount(form.discount).value, amount(form.markup).value)
)

/** То же, когда итог уже посчитан экраном: считать его дважды незачем. */
fun saleStateOf(session: Session, basket: Basket, form: SaleForm, total: BigDecimal): SaleState = SaleState(
    hasKkm = session.selected != null,
    kkmBlocked = session.selected?.isBlocked == true,
    hasPin = session.pin.isNotEmpty(),
    shiftOpen = session.shiftOpen,
    positions = basket.positions.size,
    hasItemDiscount = basket.hasItemDiscount,
    hasZeroPrice = basket.hasZeroPrice,
    receiptDiscount = amount(form.discount).value,
    receiptMarkup = amount(form.markup).value,
    total = total,
    paymentCodes = form.split.types,
    splitIssue = form.split.issue(total),
    unsupportedPayments = unsupportedPayments(session),
    taken = amount(form.taken).value,
    cashSum = form.split.cashSum(total),
    customerBin = form.customerBin,
    missingDomainField = form.domain.missing
)
