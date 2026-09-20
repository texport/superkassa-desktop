package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetReceiptDetails
import kz.mybrain.superkassa.desktop.ui.components.HeroSumLine
import kz.mybrain.superkassa.desktop.ui.components.MinorSumLine
import kz.mybrain.superkassa.desktop.ui.components.NamedSumRow
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs

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
            note = "${cabinetQuantity(item.quantity)} × ${cabinetSum(item.price)}",
            amount = cabinetSum(item.sum)
        )
    }
    if (receipt.payments.isNotEmpty()) {
        BreakdownTitle(texts.receiptPayments)
        receipt.payments.forEach { payment ->
            NamedSumRow(name = paymentTitle(payment.type, texts), amount = cabinetSum(payment.sum))
        }
    }
    if (receipt.taxes.isNotEmpty()) {
        BreakdownTitle(texts.receiptTaxes)
        receipt.taxes.forEach { tax ->
            NamedSumRow(
                name = listOfNotNull(taxTitle(tax.type, texts), tax.percent?.let { "$it %" })
                    .joinToString(Glyphs.SEPARATOR),
                amount = cabinetSum(tax.sum)
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
    amounts?.discount?.let { MinorSumLine(texts.receiptDiscount, cabinetSum(it)) }
    amounts?.markup?.let { MinorSumLine(texts.receiptMarkup, cabinetSum(it)) }
    amounts?.taken?.let { MinorSumLine(texts.receiptTaken, cabinetSum(it)) }
    amounts?.change?.let { MinorSumLine(texts.receiptChange, cabinetSum(it)) }
    HeroSumLine(
        title = texts.receiptTotal,
        amount = cabinetSum(amounts?.total ?: receipt.total),
        color = MaterialTheme.colorScheme.onSurface
    )
}

/** Подпись части чека: отделяет позиции от оплаты и налогов. */
@Composable
private fun BreakdownTitle(title: String) {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Text(text = title, style = MaterialTheme.typography.titleSmall)
}
