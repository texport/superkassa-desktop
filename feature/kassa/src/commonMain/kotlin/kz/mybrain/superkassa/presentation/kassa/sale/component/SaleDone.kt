package kz.mybrain.superkassa.presentation.kassa.sale.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import kz.mybrain.superkassa.designsystem.adaptive.WrapRow
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.designsystem.text.MoneyText
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.designsystem.theme.type.MoneyStyle
import kz.mybrain.superkassa.domain.kassa.model.sale.IssuedReceipt
import kz.mybrain.superkassa.presentation.common.format.Money
import kz.mybrain.superkassa.presentation.kassa.sale.ReceiptOutput
import kz.mybrain.superkassa.presentation.words.kassa.title
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Итог пробитого чека: сумма, сдача и что сделать с чеком.
 *
 * Стоит на месте пустой корзины, пока кассир не начал следующий чек:
 * сумму и сдачу называют покупателю вслух, а чек ему показывают или
 * печатают. Прежде после чека оставалась только строка сообщений,
 * и сдачу кассир держал в уме. Сдача — самым крупным числом: ошибка
 * в ней стоит живых денег.
 */
@Composable
internal fun IssuedCard(
    issued: IssuedReceipt,
    output: ReceiptOutput,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val texts = textsOf(LocalLanguage.current).kassa.checkout
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.blockPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.fieldGap)
        ) {
            Text(
                text = "${texts.issued}: ${issued.operation.title(LocalStrings.current.receipt)}",
                style = MaterialTheme.typography.titleLarge
            )
            Figure(texts.issuedSum, issued.total, MoneyStyle.row)
            issued.change?.takeIf { it > 0L }?.let { Figure(texts.change, it, MoneyStyle.hero) }
            IssuedActions(issued.documentId, output, texts.showReceipt, texts.printReceipt)
            TextButton(onClick = onNext) { Text(texts.nextReceipt) }
        }
    }
}

/** Подпись и сумма под ней. */
@Composable
private fun Figure(label: String, tiyn: Long, style: TextStyle) {
    Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    MoneyText(Money.formatTiyn(tiyn), Modifier.fillMaxWidth(), style)
}

/** Показать и распечатать: тональная и обводная, главное действие экрана — не здесь. */
@Composable
private fun IssuedActions(documentId: String, output: ReceiptOutput, show: String, print: String) {
    WrapRow(spacing = Spacing.buttonGap) {
        output.show?.let { open ->
            FilledTonalButton(onClick = { open(documentId) }) { Labelled(AppIcons.preview, show) }
        }
        output.print?.let { send ->
            OutlinedButton(onClick = { send(documentId) }) { Labelled(AppIcons.print, print) }
        }
    }
}

@Composable
private fun Labelled(icon: ImageVector, text: String) {
    Icon(icon, contentDescription = null, modifier = Modifier.padding(end = ButtonDefaults.IconSpacing))
    Text(text)
}
