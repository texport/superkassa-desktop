package kz.mybrain.superkassa.presentation.cabinet.documents.component

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.designsystem.section.HeroSumLine
import kz.mybrain.superkassa.designsystem.section.MinorSumLine
import kz.mybrain.superkassa.designsystem.section.NamedSumRow
import kz.mybrain.superkassa.designsystem.theme.icon.Glyphs
import kz.mybrain.superkassa.domain.cabinet.model.documents.CabinetReceiptDetails
import kz.mybrain.superkassa.presentation.cabinet.documents.paymentTitle
import kz.mybrain.superkassa.presentation.cabinet.documents.taxTitle
import kz.mybrain.superkassa.presentation.common.format.Money
import kz.mybrain.superkassa.strings.api.cabinet.CabinetTexts

/**
 * Состав чека: позиции, оплата, налоги и итог.
 *
 * Порядок сверху вниз повторяет бумажный чек: за что, чем расплатились,
 * сколько налога и сколько вышло. Вид оплаты и налог названы словами —
 * прежде здесь стояли коды протокола `CASH` и `VAT`.
 */
@Composable
fun ReceiptBreakdown(receipt: CabinetReceiptDetails, texts: CabinetTexts) {
    BreakdownTitle(texts.receiptItems)
    receipt.items.forEach { item ->
        NamedSumRow(
            name = item.name.orEmpty(),
            note = "${Money.quantity(item.quantity)} × ${Money.format(item.price)}",
            amount = Money.format(item.sum)
        )
    }
    if (receipt.payments.isNotEmpty()) {
        BreakdownTitle(texts.receiptPayments)
        receipt.payments.forEach { payment ->
            NamedSumRow(name = paymentTitle(payment.type, texts), amount = Money.format(payment.sum))
        }
    }
    if (receipt.taxes.isNotEmpty()) {
        BreakdownTitle(texts.receiptTaxes)
        receipt.taxes.forEach { tax ->
            NamedSumRow(
                name = listOfNotNull(taxTitle(tax.type, texts), tax.percent?.let { "$it %" })
                    .joinToString(Glyphs.SEPARATOR),
                amount = Money.format(tax.sum)
            )
        }
    }
    ReceiptTotals(receipt, texts)
}

/**
 * Итоги чека.
 *
 * Скидка, полученное и сдача — слагаемые: они приглушены и стоят над
 * итогом. Итог — главное число карточки и набран денежной шкалой.
 * Прежде их не было вовсе, хотя кабинет их отдаёт.
 */
@Composable
private fun ReceiptTotals(receipt: CabinetReceiptDetails, texts: CabinetTexts) {
    val amounts = receipt.amounts
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    amounts?.discount?.let { MinorSumLine(texts.receiptDiscount, Money.format(it)) }
    amounts?.markup?.let { MinorSumLine(texts.receiptMarkup, Money.format(it)) }
    amounts?.taken?.let { MinorSumLine(texts.receiptTaken, Money.format(it)) }
    amounts?.change?.let { MinorSumLine(texts.receiptChange, Money.format(it)) }
    HeroSumLine(
        title = texts.receiptTotal,
        amount = Money.format(amounts?.total ?: receipt.total),
        color = MaterialTheme.colorScheme.onSurface
    )
}

/** Подпись части чека: отделяет позиции от оплаты и налогов. */
@Composable
private fun BreakdownTitle(title: String) {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Text(text = title, style = MaterialTheme.typography.titleSmall)
}
