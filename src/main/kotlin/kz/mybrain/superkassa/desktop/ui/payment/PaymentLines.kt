package kz.mybrain.superkassa.desktop.ui.payment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.ui.components.Money
import kz.mybrain.superkassa.desktop.ui.components.MoneyField
import kz.mybrain.superkassa.desktop.ui.components.PaymentPicker
import kz.mybrain.superkassa.desktop.ui.strings.paymentTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import java.math.BigDecimal

/**
 * Чем платят за чек.
 *
 * Пока оплата одна, экран выглядит как прежде: один список видов и ничего
 * лишнего — так проходит почти каждый чек. Вторая оплата добавляется одной
 * кнопкой, и только тогда появляются суммы.
 *
 * Сумма последней оплаты не вводится, а показывается остатком: кассир
 * набирает то, что прошло по карте, остальное касса дописывает сама.
 * Складывать суммы в итог вручную под взглядом очереди — верный способ
 * ошибиться на тиын и получить отказ ОФД на уже пробитом чеке.
 *
 * @param unsupportedNote чем объясняется погасший вид оплаты в списке.
 */
@Composable
fun PaymentLines(
    session: Session,
    split: PaymentSplit,
    total: BigDecimal,
    unsupportedNote: String = "",
    modifier: Modifier = Modifier
) {
    val texts = paymentTexts(session.language)
    val entries = paymentEntries(session)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.tight)
    ) {
        split.entries.forEach { line ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PaymentPicker(
                    entries = entries,
                    language = session.language.code,
                    selectedCode = line.type,
                    onSelect = { split.retype(line, it) },
                    modifier = Modifier.weight(1f)
                )
                if (split.mixed) {
                    AmountField(split, line, total, texts.amount, texts.rest)
                    IconButton(onClick = { split.remove(line) }) {
                        Icon(AppIcons.close, contentDescription = texts.removePayment)
                    }
                }
            }
        }
        // Почему вид в списке погас — один раз под всеми строками:
        // в каждой строке это была бы одна и та же фраза трижды.
        // И только когда погасшие виды в списке есть: без этого условия
        // касса писала «протокол его не принимает» под наличными,
        // то есть под видом оплаты, который принимают все версии.
        if (unsupportedNoteVisible(unsupportedNote, entries)) {
            Text(
                text = unsupportedNote,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        val free = entries.filter { it.supported && it.code !in split.types }
        if (free.isNotEmpty()) {
            TextButton(onClick = { split.add(free.first().code) }) {
                Icon(AppIcons.add, contentDescription = null)
                Text(text = texts.addPayment, modifier = Modifier.padding(start = Spacing.tight))
            }
        }
    }
}

/**
 * Сумма одной оплаты.
 *
 * Остаток чека берёт наличная строка, а без наличных — последняя: поле
 * только показывает остаток и краснеет, когда по прочим видам расписано
 * больше итога.
 */
@Composable
private fun AmountField(
    split: PaymentSplit,
    line: PaymentLine,
    total: BigDecimal,
    amountLabel: String,
    restLabel: String
) {
    val takesRest = split.takesRest(line)
    val rest = total - split.assigned()
    MoneyField(
        value = if (takesRest) Money.entered(rest) else line.amount,
        label = if (takesRest) restLabel else amountLabel,
        modifier = Modifier.width(Sizes.fieldPrice),
        isError = if (takesRest) rest.signum() <= 0 else line.amount.isNotBlank() && line.value == null,
        readOnly = takesRest,
        onValueChange = { if (!takesRest) line.amount = it }
    )
}
