package kz.mybrain.superkassa.desktop.ui.returns

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.Dictionary
import kz.mybrain.superkassa.desktop.server.Document
import kz.mybrain.superkassa.desktop.server.ReceiptPayment
import kz.mybrain.superkassa.desktop.server.SoldItem
import kz.mybrain.superkassa.desktop.server.buyReturn
import kz.mybrain.superkassa.desktop.server.documentDetails
import kz.mybrain.superkassa.desktop.server.sellReturn
import kz.mybrain.superkassa.desktop.ui.components.EmptyState
import kz.mybrain.superkassa.desktop.ui.components.FieldButton
import kz.mybrain.superkassa.desktop.ui.components.FieldButtonKind
import kz.mybrain.superkassa.desktop.ui.components.Money
import kz.mybrain.superkassa.desktop.ui.components.fieldWidth
import kz.mybrain.superkassa.desktop.ui.history.DASH
import kz.mybrain.superkassa.desktop.ui.payment.PaymentLines
import kz.mybrain.superkassa.desktop.ui.payment.PaymentSplit
import kz.mybrain.superkassa.desktop.ui.payment.SplitIssue
import kz.mybrain.superkassa.desktop.ui.strings.AppStrings
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.PaymentTexts
import kz.mybrain.superkassa.desktop.ui.strings.ReturnJournalTexts
import kz.mybrain.superkassa.desktop.ui.strings.journalTexts
import kz.mybrain.superkassa.desktop.ui.strings.paymentTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.MoneyStyle
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Сумма возврата и само действие.
 *
 * Панель поднята над списком: она — то, ради чего экран открыт, и одно
 * главное действие живёт здесь. Поле заполняется суммой чека целиком —
 * это обычный случай, — но менять её можно: покупатель возвращает один
 * товар из трёх. Больше суммы чека вернуть нельзя, и говорится об этом
 * до отправки.
 */
@Composable
fun RefundPanel(
    session: Session,
    kind: ReturnKind,
    basis: Document?,
    modifier: Modifier,
    onDone: () -> Unit
) {
    val journal = journalTexts(session.language).returns
    ElevatedCard(modifier = modifier.fillMaxHeight()) {
        if (basis == null) {
            EmptyState(
                icon = AppIcons.noBasis,
                title = journal.chooseBasis,
                hint = journal.chooseBasisHint,
                modifier = Modifier.fillMaxHeight()
            )
        } else {
            RefundForm(session, journal, kind, basis, onDone)
        }
    }
}

/** Ввод суммы и отправка. Ключ повтора живёт, пока выбран тот же чек. */
@Composable
private fun RefundForm(
    session: Session,
    journal: ReturnJournalTexts,
    kind: ReturnKind,
    basis: Document,
    onDone: () -> Unit
) {
    val texts = LocalStrings.current
    val total = basis.totalAmount ?: 0L
    var entered by remember(basis.id) { mutableStateOf(tengeText(total)) }
    val split = remember(basis.id) { PaymentSplit(CASH) }
    // Ключ живёт, пока идёт работа с одним и тем же чеком-основанием:
    // повтор после молчания узла не должен применить возврат дважды.
    val key = remember(basis.id) { refundKey() }
    var working by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val checked = refundAmountOf(entered, total)
    // Оплаты разбиваются от суммы возврата: пока сумма не принята,
    // разбивать нечего, и остаток в строках стоит нулём.
    val refundSum = Money.tengeOf((checked as? RefundAmount.Ready)?.tiyn ?: 0L)
    // Состав чека приходит от узла: вернуть можно только то, что продано,
    // и теми же строками, какими продано.
    var items by remember(basis.id) { mutableStateOf(emptyList<SoldItem>()) }
    var chosen by remember(basis.id) { mutableStateOf(emptySet<Int>()) }
    LaunchedEffect(basis.id) {
        val kkm = session.selected ?: return@LaunchedEffect
        items = session.guard(journal.basis) {
            session.client.documentDetails(kkm.kkmId, basis.id, session.pin).items
        }.orEmpty()
        chosen = emptySet()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.normal),
        verticalArrangement = Arrangement.spacedBy(Spacing.snug)
    ) {
        Summary(basis, journal, total)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        RefundItems(items, chosen, journal) { at ->
            chosen = if (at in chosen) chosen - at else chosen + at
            // Отметка позиции задаёт сумму: считать её руками кассир
            // не должен, а поправить поле по-прежнему может.
            entered = tengeText(if (chosen.isEmpty()) total else chosenTiyn(items, chosen))
        }
        AmountRow(journal, entered, checked is RefundAmount.Rejected, { entered = it }) {
            chosen = emptySet()
            entered = tengeText(total)
        }
        // Возврат отдают тем же набором, каким платили: часть на карту,
        // часть из ящика. Сумма разбивается от суммы возврата, а не от
        // итога чека-основания.
        PaymentLines(session, split, refundSum)
        Hints(journal, checked, split.issue(refundSum), paymentTexts(session.language))
        Spacer(modifier = Modifier.weight(1f))
        Button(
            enabled = !working && checked is RefundAmount.Ready && split.issue(refundSum) == null,
            onClick = {
                val ready = checked as? RefundAmount.Ready ?: return@Button
                working = true
                scope.launch {
                    // Удался — выбор снимается: сумма чека в поле после
                    // частичного возврата приглашала бы вернуть его ещё раз.
                    val returned = chosen.mapNotNull { items.getOrNull(it) }
                    val payments = split.toPayments(refundSum)
                    if (refund(session, texts, kind, basis, ready.tiyn, payments, key, returned)) {
                        onDone()
                    }
                    working = false
                }
            },
            modifier = Modifier.fillMaxWidth().height(Sizes.fieldHeight)
        ) {
            Text(kind.action(texts.returns), style = MaterialTheme.typography.titleMedium)
        }
    }
}

/**
 * Чек-основание в лицо: номер, сумма крупно, фискальный признак.
 *
 * Сумма чека — главное число панели, и набрана она тем же начертанием,
 * что итог чека на экране продажи: кассир сверяет её с бумагой в руке.
 */
@Composable
private fun Summary(basis: Document, journal: ReturnJournalTexts, total: Long) {
    val texts = LocalStrings.current
    Text(
        text = "${journal.basis}: ${texts.returns.receiptNo} ${basis.docNo}",
        style = MaterialTheme.typography.titleMedium
    )
    Text(
        text = journal.receiptTotal,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(Money.formatTiyn(total), style = MoneyStyle.hero, modifier = Modifier.fillMaxWidth())
    Text(
        text = "${journal.fiscalSign}: ${basis.fiscalSign ?: basis.autonomousSign ?: DASH}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** Сумма возврата: моноширинно и вправо, как и всякая сумма в кассе. */
@Composable
private fun AmountRow(
    journal: ReturnJournalTexts,
    entered: String,
    rejected: Boolean,
    onEnter: (String) -> Unit,
    onWholeReceipt: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
        verticalAlignment = Alignment.Top
    ) {
        OutlinedTextField(
            value = entered,
            onValueChange = onEnter,
            label = { Text(journal.amount) },
            singleLine = true,
            isError = rejected,
            textStyle = MoneyStyle.row,
            modifier = Modifier.fieldWidth(journal.amount, Sizes.fieldAmount)
        )
        FieldButton(journal.wholeReceipt, FieldButtonKind.Text, onClick = onWholeReceipt)
    }
}

/** Пояснение о частичном возврате и, если есть, отказ по введённой сумме. */
@Composable
private fun Hints(
    journal: ReturnJournalTexts,
    checked: RefundAmount,
    split: SplitIssue?,
    payment: PaymentTexts
) {
    Text(
        text = journal.partialHint,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    val problem = when {
        checked is RefundAmount.Rejected -> problemText(checked.reason, journal)
        split == SplitIssue.Empty -> payment.splitEmpty
        split == SplitIssue.Excess -> payment.splitExcess
        else -> null
    }
    if (problem != null) {
        Text(
            text = problem,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
    }
}

private fun problemText(reason: RefundProblem, journal: ReturnJournalTexts): String = when (reason) {
    RefundProblem.Empty -> journal.amountEmpty
    RefundProblem.NotANumber -> journal.amountInvalid
    RefundProblem.NotPositive -> journal.amountEmpty
    RefundProblem.TooLarge -> journal.amountTooLarge
}

/**
 * Оформляет возврат и сообщает исход словами узла.
 *
 * Состояние доставки берётся из справочника узла: прежде здесь стоял
 * голый код протокола, и кассир читал «ONLINE_OK» вместо «Доставлен».
 */
private suspend fun refund(
    session: Session,
    texts: AppStrings,
    kind: ReturnKind,
    basis: Document,
    refundTiyn: Long,
    payments: List<ReceiptPayment>,
    key: String,
    returned: List<SoldItem>
): Boolean {
    val kkm = session.selected ?: return false
    val request = refundRequest(
        basis = basis,
        kgdKkmId = kkm.kkmKgdId.orEmpty(),
        refundTiyn = refundTiyn,
        idempotencyKey = key,
        lineName = "${texts.returns.refundFor} ${basis.docNo}",
        payments = payments,
        returned = returned
    ) ?: return false
    val result = session.guard(kind.title(texts.returns)) {
        when (kind) {
            ReturnKind.Sell -> session.client.sellReturn(kkm.kkmId, request, session.pin)
            ReturnKind.Buy -> session.client.buyReturn(kkm.kkmId, request, session.pin)
        }
    } ?: return false
    session.report(
        "${kind.title(texts.returns)} ${texts.returns.done}: " +
            "${session.titleOf(Dictionary.DeliveryStatuses, result.deliveryStatus)} · ${Money.formatTiyn(refundTiyn)}"
    )
    session.refreshSelected()
    return true
}

private fun refundKey(): String = "desktop-return-${System.currentTimeMillis()}"

/** Вид оплаты возврата по умолчанию: чаще всего деньги отдают из ящика. */
private const val CASH = "CASH"
