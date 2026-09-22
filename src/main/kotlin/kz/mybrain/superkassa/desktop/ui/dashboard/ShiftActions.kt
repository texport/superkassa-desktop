package kz.mybrain.superkassa.desktop.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.app.ShiftState
import kz.mybrain.superkassa.desktop.app.refreshKkms
import kz.mybrain.superkassa.desktop.app.refreshSelected
import kz.mybrain.superkassa.desktop.server.FiscalResult
import kz.mybrain.superkassa.desktop.server.Kkm
import kz.mybrain.superkassa.desktop.server.closeShift
import kz.mybrain.superkassa.desktop.server.openShift
import kz.mybrain.superkassa.desktop.server.xReport
import kz.mybrain.superkassa.desktop.ui.components.ConfirmActionDialog
import kz.mybrain.superkassa.desktop.ui.components.Money
import kz.mybrain.superkassa.desktop.ui.strings.CommonStrings
import kz.mybrain.superkassa.desktop.ui.strings.DashboardStrings
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.blockReasonTexts
import kz.mybrain.superkassa.desktop.ui.strings.blockReasonWords
import kz.mybrain.superkassa.desktop.ui.strings.moneyTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Управление сменой.
 *
 * Закрытие смены — это Z-отчёт, и назван он так, как называет его кассир,
 * а не протокол.
 */
@Composable
internal fun ShiftActions(session: Session) {
    val texts = LocalStrings.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var asking by remember { mutableStateOf(false) }
    val programming = session.selected?.isProgramming == true
    // Заблокированная касса — и снятая с учёта в том числе — фискальных
    // команд не принимает: узел отвечает KKM_BLOCKED. Кнопки ей не
    // показываются вовсе, а вместо них написано почему.
    val blocked = session.selected?.isBlocked == true
    // Состояние смены, которого узел не назвал, не повод предлагать
    // действие: «Открыть смену» над сменой, открытой на узле, получало
    // SHIFT_ALREADY_OPEN.
    val known = session.shiftState != ShiftState.Unknown
    // В режиме программирования узел фискальных команд не принимает, и смену
    // он в нём не показывает. Предлагать «Открыть смену» поверх открытой
    // смены — толкать кассира на отказ.
    val enabled = !busy && session.selected != null && session.pin.isNotEmpty() && !programming

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.tight)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Главное действие экрана одно и зависит от состояния смены:
            // закрытую открывают, открытую закрывают. Остальное — тональное.
            val offer = !blocked && known
            if (offer && session.shiftOpen) {
                // Z-отчёт не отменяется, и до вопроса он снимался с одного
                // нажатия — тогда как внесение денег в ящик спрашивало.
                Button(enabled = enabled, onClick = { asking = true }) { Text(texts.dashboard.closeShift) }

                FilledTonalButton(enabled = enabled, onClick = {
                    busy = true
                    scope.launch {
                        run(session, texts.dashboard.xReport, texts.dashboard.xReportDone, texts.common) {
                            session.client.xReport(it.kkmId, session.pin)
                        }
                        busy = false
                    }
                }) { Text(texts.dashboard.xReport) }
            } else if (offer && session.isAdmin) {
                Button(enabled = enabled, onClick = {
                    busy = true
                    scope.launch {
                        run(session, texts.dashboard.openShift, texts.dashboard.shiftOpened, texts.common) {
                            session.client.openShift(it.kkmId, session.pin)
                        }
                        busy = false
                    }
                }) { Text(texts.dashboard.openShift) }
            }
        }
        // Сутки открытой смены — причина, по которой узел блокирует кассу.
        // До правки об этом узнавали из отказа на первом чеке следующего
        // утра: экран молчал о смене, идущей вторые сутки.
        if (!blocked && shiftTooLong(session.shiftOpenedAt, System.currentTimeMillis())) {
            Text(
                texts.dashboard.shiftTooLong,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        if (blocked) {
            // Причина блокировки — словами и с тем, что делать: одна фраза
            // на все случаи отправляла кассира с отозванным токеном
            // разбираться со снятием с учёта.
            val reason = blockReasonWords(session.selected?.blockReasonCode, session.language)
            Text(
                "$reason${Glyphs.SEPARATOR}${blockReasonTexts(session.language).readingStays}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (programming) {
            Text(
                texts.settings.enteredProgramming,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (known && !session.shiftOpen && !session.isAdmin) {
            // Смену открывает администратор: кассиру вместо кнопки,
            // на которую узел ответит отказом, сказано, кого позвать.
            // Администратору строка не нужна — у него есть сама кнопка,
            // а «откройте смену» уже написано в пустом списке документов.
            Text(
                texts.dashboard.openShiftAdmin,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (asking) {
        ConfirmActionDialog(
            icon = AppIcons.shiftClose,
            what = texts.dashboard.closeShiftAsk,
            explain = closeShiftExplain(session, texts.dashboard),
            action = texts.dashboard.closeShift,
            cancel = moneyTexts(session.language).drawer.cancel,
            busy = busy,
            onCancel = { asking = false }
        ) {
            busy = true
            scope.launch {
                run(session, texts.dashboard.closeShift, texts.dashboard.shiftClosedDone, texts.common) {
                    session.client.closeShift(it.kkmId, session.pin)
                }
                busy = false
                asking = false
            }
        }
    }
}

/**
 * Что кассир прочитает перед Z-отчётом.
 *
 * Число документов и остаток в ящике — то же, что в плитках над кнопкой:
 * решение принимают по ним. Судьба остатка названа отдельно, потому что
 * зависит от настройки кассы, а не от того, что кассир видит на экране.
 */
private fun closeShiftExplain(session: Session, texts: DashboardStrings): String {
    val cash = if (session.selected?.autoCashout == true) texts.closeShiftCashout else texts.closeShiftKeepsCash
    return texts.closeShiftExplain.format(
        session.documents.size.toString(),
        Money.formatTiyn(session.cashInDrawer)
    ) + " " + cash
}

/**
 * Выполняет действие над сменой и объявляет кассиру, чем оно кончилось.
 *
 * Итог собирается из надписей словаря: то же сообщение по-казахски или
 * по-английски нельзя собрать, дописав русский хвост к переведённому
 * началу.
 */
private suspend fun run(
    session: Session,
    doing: String,
    done: String,
    texts: CommonStrings,
    action: suspend (Kkm) -> FiscalResult
) {
    val kkm = session.selected ?: return
    // Действие называется тем, что делается, а не тем, что получится:
    // при недоступном узле сообщение «узел недоступен · смена открыта»
    // говорило кассиру ровно обратное правде.
    val result = session.guard(doing) { action(kkm) } ?: return
    session.report(
        when {
            result.isDelivered -> "$done: ${texts.deliveredToOfd}"
            result.isQueued -> "$done: ${texts.queuedNoLink}"
            result.deliveryStatus == null -> done
            else -> "$done. ${texts.deliveryState}: ${result.deliveryStatus}"
        }
    )
    session.refreshKkms()
    session.refreshSelected()
}

/**
 * Смена идёт дольше суток.
 *
 * Считается по часам узла, а не по сроку жизни экрана: смену открыли
 * вчера и за машиной с тех пор сменился кассир. Ровно сутки — уже предел:
 * по нему узел и блокирует кассу.
 */
internal fun shiftTooLong(openedAt: Long?, now: Long): Boolean =
    openedAt != null && now - openedAt >= DAY

/** Сутки в миллисекундах: столько узел держит смену открытой. */
private const val DAY = 24L * 60 * 60 * 1000
