package kz.mybrain.superkassa.desktop.ui.cash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.app.refreshSelected
import kz.mybrain.superkassa.desktop.app.titleOf
import kz.mybrain.superkassa.desktop.server.CashRequest
import kz.mybrain.superkassa.desktop.server.Dictionary
import kz.mybrain.superkassa.desktop.server.Document
import kz.mybrain.superkassa.desktop.server.cashIn
import kz.mybrain.superkassa.desktop.server.cashOut
import kz.mybrain.superkassa.desktop.ui.components.InfoTip
import kz.mybrain.superkassa.desktop.ui.components.Money
import kz.mybrain.superkassa.desktop.ui.components.ScreenTitle
import kz.mybrain.superkassa.desktop.ui.components.ScrollableColumn
import kz.mybrain.superkassa.desktop.ui.strings.AppStrings
import kz.mybrain.superkassa.desktop.ui.strings.DrawerTexts
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.MoneyTexts
import kz.mybrain.superkassa.desktop.ui.strings.moneyTexts
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs
import kz.mybrain.superkassa.desktop.ui.theme.MoneyStyle
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import java.math.BigDecimal

/**
 * Денежный ящик: остаток, внесение, изъятие и что уже проведено.
 *
 * Остаток берётся из счётчика узла — того же, по которому узел решает,
 * хватает ли денег на изъятие. Свой подсчёт по документам разошёлся бы
 * с ним, и касса отказывала бы в изъятии денег, которые по её же экрану
 * в ящике есть.
 */
@Composable
fun CashScreen(session: Session) {
    val texts = LocalStrings.current
    val money = moneyTexts(session.language)
    val recent = remember { mutableStateListOf<Document>() }
    // Журнал за сутки читается у узла: до ответа «внесений и изъятий нет»
    // говорило бы о дне, про который ещё не спрашивали.
    var answered by remember(session.selected?.kkmId) { mutableStateOf(false) }

    LaunchedEffect(session.selected?.kkmId, session.pin) {
        reloadRecent(session, money, recent)
        answered = true
    }

    ScrollableColumn(
        modifier = Modifier.fillMaxSize().padding(Spacing.screen),
        spacing = Spacing.normal
    ) {
        ScreenTitle(money.drawer.inDrawer)
        DrawerCard(session, money.drawer)
        CashForm(session, money) { move, amount, key ->
            perform(session, texts, money, move, amount, key, recent)
        }
        RecentCash(session, money.drawer, recent, loading = !answered)
    }
}

/**
 * Остаток ящика — главное число экрана.
 *
 * Подпись сверху, сумма под ней крупно и вправо, объяснение снизу: кассир
 * читает остаток с метра, а объяснение — только когда сумма его удивила.
 * Состояние смены здесь не повторяется: оно стоит плашкой в шапке окна,
 * одной на все разделы.
 */
@Composable
private fun DrawerCard(session: Session, drawer: DrawerTexts) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.roomy),
            verticalArrangement = Arrangement.spacedBy(Spacing.tight)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = drawer.inDrawer,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                InfoTip(drawer.countedByNode)
            }
            Text(
                text = Money.formatTiyn(session.cashInDrawer),
                style = MoneyStyle.hero,
                modifier = Modifier.fillMaxWidth()
            )
            // Пока остаток неизвестен, об этом сказано строкой: это помеха.
            // Объяснение, кто его считает, ушло под значок у подписи.
            if (session.cashInDrawer == null) {
                Text(
                    text = drawer.unknownBalance,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Проводит внесение или изъятие.
 *
 * Порядок сообщений намеренный: сначала перечитывается состояние кассы,
 * и только потом объявляется итог операции. Перечитывание снимает
 * сообщение за собой, и объяви мы итог раньше — кассир остался бы без
 * подтверждения того, что деньги проведены.
 */
private suspend fun perform(
    session: Session,
    texts: AppStrings,
    money: MoneyTexts,
    move: CashMove,
    amount: BigDecimal,
    idempotencyKey: String,
    recent: MutableList<Document>
): Boolean {
    val kkm = session.selected ?: return false
    val what = if (move == CashMove.Deposit) texts.cash.deposit else texts.cash.withdraw
    val request = CashRequest(amount = amount, idempotencyKey = idempotencyKey)
    val result = session.guard(what) {
        if (move == CashMove.Deposit) {
            session.client.cashIn(kkm.kkmId, request, session.pin)
        } else {
            session.client.cashOut(kkm.kkmId, request, session.pin)
        }
    } ?: return false
    session.refreshSelected()
    reloadRecent(session, money, recent)
    val done = if (move == CashMove.Deposit) texts.cash.deposited else texts.cash.withdrawn
    val delivery = session.titleOf(Dictionary.DeliveryStatuses, result.deliveryStatus)
    session.report("$done ${Money.format(amount)}${Glyphs.SEPARATOR}$delivery")
    return true
}
