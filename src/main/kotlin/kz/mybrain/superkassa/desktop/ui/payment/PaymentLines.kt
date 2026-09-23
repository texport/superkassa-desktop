package kz.mybrain.superkassa.desktop.ui.payment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.ui.adaptive.WrapRow
import kz.mybrain.superkassa.desktop.ui.components.Money
import kz.mybrain.superkassa.desktop.ui.components.MoneyField
import kz.mybrain.superkassa.desktop.ui.components.PaymentPicker
import kz.mybrain.superkassa.desktop.ui.strings.paymentTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs
import kz.mybrain.superkassa.desktop.ui.theme.KassaLayout
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
        // Сумма, вид и удаление делят строку, пока сумме хватает места
        // целиком; нет — сумма занимает строку одна, а вид с удалением
        // встают под ней. Постоянной ширины поле резало суммы от миллиарда
        // даже в самом широком окне.
        split.entries.forEach { line ->
            WrapRow(modifier = Modifier.fillMaxWidth()) {
                if (split.mixed) AmountField(split, line, total, texts.amount, texts.rest)
                PaymentPicker(
                    entries = entries,
                    language = session.language.code,
                    selectedCode = line.type,
                    onSelect = { split.retype(line, it) },
                    modifier = Modifier.weight(1f).widthIn(min = Sizes.fieldChoice)
                )
                if (split.mixed) {
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
 * больше итога. Остаток набран как всякая сумма кассы — разрядами и знаком
 * минуса: отрицательный он был единственной суммой с дефисом и без разрядов.
 */
@Composable
private fun FlowRowScope.AmountField(
    split: PaymentSplit,
    line: PaymentLine,
    total: BigDecimal,
    amountLabel: String,
    restLabel: String
) {
    val takesRest = split.takesRest(line)
    val rest = total - split.assigned()
    MoneyField(
        value = if (takesRest) restShown(rest) else line.amount,
        label = if (takesRest) restLabel else amountLabel,
        modifier = Modifier.weight(1f).widthIn(min = KassaLayout.paymentAmount),
        isError = if (takesRest) rest.signum() <= 0 else line.amount.isNotBlank() && line.value == null,
        readOnly = takesRest,
        onValueChange = { if (!takesRest) line.amount = it }
    )
}

/**
 * Остаток так, как его показывает поле: разрядами и знаком минуса, как
 * всякая сумма кассы, но без знака валюты — его нет и у набранных сумм
 * в соседних полях.
 */
private fun restShown(rest: BigDecimal): String =
    Money.format(rest).removeSuffix("${Glyphs.NBSP}${Glyphs.TENGE}")
