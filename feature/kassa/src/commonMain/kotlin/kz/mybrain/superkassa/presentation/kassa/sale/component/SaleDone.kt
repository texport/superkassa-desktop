package kz.mybrain.superkassa.presentation.kassa.sale.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.domain.kassa.model.sale.IssuedReceipt
import kz.mybrain.superkassa.presentation.kassa.payment.DocumentDoneCard
import kz.mybrain.superkassa.presentation.kassa.payment.ReceiptOutput
import kz.mybrain.superkassa.presentation.words.kassa.title

/** Итог пробитого чека продажи или покупки — на месте пустой корзины. */
@Composable
internal fun IssuedCard(
    issued: IssuedReceipt,
    output: ReceiptOutput,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val kind = issued.operation.title(LocalStrings.current.receipt)
    DocumentDoneCard(kind, issued.total, issued.change, issued.documentId, output, onNext, modifier)
}
