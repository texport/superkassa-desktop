package kz.mybrain.superkassa.presentation.kassa.payment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.texport.superkassa.core.presentation.api.model.reference.PaymentTypeResponse
import kz.mybrain.superkassa.domain.kassa.model.payment.PaymentSplit
import kz.mybrain.superkassa.presentation.common.field.MoneyField
import kz.mybrain.superkassa.presentation.common.format.Money
import kz.mybrain.superkassa.presentation.common.strings.LocalLanguage
import kz.mybrain.superkassa.presentation.kassa.payment.component.PaymentPicker
import kz.mybrain.superkassa.presentation.theme.icon.AppIcons
import kz.mybrain.superkassa.presentation.theme.icon.Glyphs
import kz.mybrain.superkassa.presentation.theme.size.Spacing
import kz.mybrain.superkassa.strings.api.textsOf

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
 * @param entries виды оплаты из справочника кассы.
 * @param unsupportedNote чем объясняется погасший вид оплаты в списке.
 */
@Composable
fun PaymentLines(
    split: PaymentSplit,
    entries: List<PaymentTypeResponse>,
    total: Long,
    actions: PaymentActions,
    unsupportedNote: String = ""
) {
    val texts = textsOf(LocalLanguage.current).kassa.payment
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.itemGap)
    ) {
        // Вид с удалением — строкой, сумма — под ними во всю ширину панели.
        split.entries.indices.forEach { at -> PaymentRow(split, at, entries, total, actions) }
        // Почему вид в списке погас — один раз под всеми строками,
        // и только когда погасшие виды в списке есть: без этого условия
        // касса писала «протокол его не принимает» под наличными.
        if (unsupportedNoteVisible(unsupportedNote, entries)) {
            Text(
                text = unsupportedNote,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        val free = entries.filter { it.supported && it.code !in split.types }
        if (free.isNotEmpty()) {
            TextButton(onClick = { actions.addPayment(free.first().code) }) {
                Icon(AppIcons.add, contentDescription = null)
                Text(text = texts.addPayment, modifier = Modifier.padding(start = Spacing.itemGap))
            }
        }
    }
}

@Composable
private fun PaymentRow(
    split: PaymentSplit,
    at: Int,
    entries: List<PaymentTypeResponse>,
    total: Long,
    actions: PaymentActions
) {
    val texts = textsOf(LocalLanguage.current).kassa.payment
    // Вид оплаты и его снятие — рядом, сумма — под ними во всю ширину:
    // суммы от миллиона не помещались в половину узкой кассы, а края полей
    // совпадают с краями прочих полей панели.
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.fieldGap)) {
        PaymentPicker(
            entries = entries,
            selectedCode = split.entries[at].type,
            onSelect = { actions.retypePayment(at, it) },
            modifier = Modifier.weight(1f)
        )
        if (split.mixed) {
            IconButton(onClick = { actions.removePayment(at) }) {
                Icon(AppIcons.close, contentDescription = texts.removePayment)
            }
        }
    }
    if (split.mixed) AmountField(split, at, total, actions)
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
private fun AmountField(split: PaymentSplit, at: Int, total: Long, actions: PaymentActions) {
    val texts = textsOf(LocalLanguage.current).kassa.payment
    val line = split.entries[at]
    val takesRest = split.takesRest(at)
    val rest = total - split.assigned()
    MoneyField(
        value = if (takesRest) restShown(rest) else line.amount,
        label = if (takesRest) texts.rest else texts.amount,
        modifier = Modifier.fillMaxWidth(),
        isError = if (takesRest) rest <= 0L else line.amount.isNotBlank() && line.value == null,
        readOnly = takesRest,
        onValueChange = { actions.enterPayment(at, it) }
    )
}

/**
 * Остаток так, как его показывает поле: разрядами и знаком минуса, как
 * всякая сумма кассы, но без знака валюты — его нет и у набранных сумм
 * в соседних полях.
 */
private fun restShown(rest: Long): String =
    Money.formatTiyn(rest).removeSuffix("${Glyphs.NBSP}${Glyphs.TENGE}")

/**
 * Показывать ли пояснение о погасших видах оплаты.
 *
 * Пояснение относится к погасшим строкам списка, а не к выбранному виду.
 * Без этого условия касса писала «протокол 2.0.4 его не принимает» под
 * наличными — видом, который принимают все версии протокола, — и кассир
 * читал это как отказ принять деньги.
 *
 * @param note сама фраза; пустая — пояснения нет.
 * @param entries виды оплаты из справочника кассы.
 */
fun unsupportedNoteVisible(note: String, entries: List<PaymentTypeResponse>): Boolean =
    note.isNotBlank() && entries.any { !it.supported }
