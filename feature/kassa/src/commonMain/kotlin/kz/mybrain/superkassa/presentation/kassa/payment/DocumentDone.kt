package kz.mybrain.superkassa.presentation.kassa.payment

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
import kz.mybrain.superkassa.designsystem.text.MoneyText
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.designsystem.theme.type.MoneyStyle
import kz.mybrain.superkassa.domain.print.model.ShareWay
import kz.mybrain.superkassa.presentation.common.format.Money
import kz.mybrain.superkassa.presentation.common.print.ShareAction
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Итог пробитого чека — продажи, покупки или возврата: сумма, сдача
 * и что сделать с чеком.
 *
 * Стоит над списком, пока кассир не начал следующий: сумму и сдачу
 * называют покупателю вслух, а чек ему показывают или печатают. Прежде
 * после чека оставалась только строка сообщений, и сдачу кассир держал
 * в уме, а чек возврата искал в журнале. Сдача — самым крупным числом:
 * ошибка в ней стоит живых денег.
 *
 * @param kind вид чека словами кассира: «Продажа», «Возврат продажи».
 * @param change сдача в тиынах; `null` или ноль — строки сдачи нет.
 */
@Composable
internal fun DocumentDoneCard(
    kind: String,
    total: Long,
    change: Long?,
    documentId: String,
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
                text = "${texts.issued}: $kind",
                style = MaterialTheme.typography.titleLarge
            )
            Figure(texts.issuedSum, total, MoneyStyle.row)
            change?.takeIf { it > 0L }?.let { Figure(texts.change, it, MoneyStyle.hero) }
            IssuedActions(documentId, output, texts.showReceipt, texts.printReceipt)
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

/**
 * Показать, распечатать и поделиться: тональная и обводные, главное действие
 * экрана — не здесь. «Поделиться» отдаёт чек тем, чем покупателю пишут
 * и так, — окном «Поделиться» Android или мессенджером компьютера.
 */
@Composable
private fun IssuedActions(documentId: String, output: ReceiptOutput, show: String, print: String) {
    WrapRow(spacing = Spacing.buttonGap) {
        output.show?.let { open ->
            FilledTonalButton(onClick = { open(documentId) }) { Labelled(AppIcons.preview, show) }
        }
        output.print?.let { send ->
            OutlinedButton(onClick = { send(documentId) }) { Labelled(AppIcons.print, print) }
        }
        ShareAction(output.shareWays, { way -> output.share(documentId, way) }) { open ->
            OutlinedButton(onClick = open) {
                Labelled(AppIcons.share, textsOf(LocalLanguage.current).common.share.share)
            }
        }
    }
}

@Composable
private fun Labelled(icon: ImageVector, text: String) {
    Icon(icon, contentDescription = null, modifier = Modifier.padding(end = ButtonDefaults.IconSpacing))
    Text(text)
}

/**
 * Что сделать с пробитым чеком: показать, распечатать, поделиться с покупателем.
 *
 * Печать — чужая продаже область, её подаёт каркас окна; без неё
 * кнопок нет, а итог чека остаётся.
 *
 * @property show открыть чек по документу кассы; `null` — показывать негде.
 * @property print распечатать чек по документу кассы; `null` — печатать негде.
 * @property shareWays пути, которыми машина делится чеком; пусто — кнопки нет.
 * @property share поделиться чеком по документу кассы выбранным путём.
 */
class ReceiptOutput(
    val show: ((String) -> Unit)? = null,
    val print: ((String) -> Unit)? = null,
    val shareWays: List<ShareWay> = emptyList(),
    val share: (String, ShareWay) -> Unit = { _, _ -> }
)
