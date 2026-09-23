package kz.mybrain.superkassa.presentation.returns

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.data.node.Document
import kz.mybrain.superkassa.data.node.SoldItem
import kz.mybrain.superkassa.data.node.documentDetails
import kz.mybrain.superkassa.presentation.components.EmptyState
import kz.mybrain.superkassa.presentation.components.Money
import kz.mybrain.superkassa.presentation.components.ScrollableColumn
import kz.mybrain.superkassa.presentation.payment.PaymentSplit
import kz.mybrain.superkassa.presentation.session.Session
import kz.mybrain.superkassa.presentation.strings.LocalStrings
import kz.mybrain.superkassa.presentation.strings.ReturnJournalTexts
import kz.mybrain.superkassa.presentation.strings.journalTexts
import kz.mybrain.superkassa.presentation.theme.AppIcons
import kz.mybrain.superkassa.presentation.theme.Spacing

/**
 * Сумма возврата и само действие.
 *
 * Панель поднята над списком: она — то, ради чего экран открыт, и одно
 * главное действие живёт здесь. Поле заполняется суммой чека целиком —
 * это обычный случай, — но менять её можно: покупатель возвращает один
 * товар из трёх. Больше суммы чека вернуть нельзя, и говорится об этом
 * до отправки.
 *
 * @param onBack вернуться к списку чеков: на узком окне панель стоит
 *   вместо списка, а не рядом с ним.
 */
@Composable
fun RefundPanel(
    session: Session,
    kind: ReturnKind,
    basis: Document?,
    modifier: Modifier,
    onBack: () -> Unit,
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
            RefundForm(session, journal, kind, basis, onBack, onDone)
        }
    }
}

/**
 * Ввод суммы и отправка. Ключ повтора живёт, пока выбран тот же чек.
 *
 * Прежде сумма и виды оплаты стояли под составом чека, и у чека
 * из пятидесяти строк их на экране не было, а кнопка оставалась
 * нажимаемой.
 */
@Composable
private fun RefundForm(
    session: Session,
    journal: ReturnJournalTexts,
    kind: ReturnKind,
    basis: Document,
    onBack: () -> Unit,
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
        // Сумма и виды оплаты стоят над составом чека, а не под ним:
        // у чека из пятидесяти строк их на экране не было, а кнопка
        // оставалась нажимаемой. Кнопка прибита к низу.
        ScrollableColumn(modifier = Modifier.weight(1f), spacing = Spacing.snug) {
            RefundSummary(basis, journal, total, onBack)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            RefundTill {
                // Отметки описывают чек возврата только тогда, когда сумма
                // осталась их суммой: поправленное поле отправит одну строку.
                val byTicks = chosen.isNotEmpty() && chosenTiyn(items, chosen) == readyTiyn(checked)
                RefundNote(journal.itemsIgnored.takeIf { chosen.isNotEmpty() && !byTicks })
                RefundAmountRow(journal, entered, checked is RefundAmount.Rejected, { entered = it }) {
                    chosen = emptySet()
                    entered = tengeText(total)
                }
                RefundMoney(session, kind, split, refundSum, journal, checked)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            RefundItems(session, items, chosen, journal) { at ->
                chosen = if (at in chosen) chosen - at else chosen + at
                // Отметка позиции задаёт сумму: считать её руками кассир
                // не должен, а поправить поле по-прежнему может.
                entered = tengeText(if (chosen.isEmpty()) total else chosenTiyn(items, chosen))
            }
        }
        RefundButton(kind, enabled = !working && checked is RefundAmount.Ready && split.issue(refundSum) == null) {
            val ready = checked as? RefundAmount.Ready ?: return@RefundButton
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
        }
    }
}

private fun refundKey(): String = "desktop-return-${System.currentTimeMillis()}"

/** Принятая сумма возврата в тиынах, а до её принятия — ноль. */
private fun readyTiyn(checked: RefundAmount): Long = (checked as? RefundAmount.Ready)?.tiyn ?: 0L

/** Вид оплаты возврата по умолчанию: чаще всего деньги отдают из ящика. */
private const val CASH = "CASH"
