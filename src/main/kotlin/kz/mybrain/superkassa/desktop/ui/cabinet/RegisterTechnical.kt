package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.server.cabinet.RegisterState
import kz.mybrain.superkassa.desktop.server.cabinet.TechnicalState
import kz.mybrain.superkassa.desktop.ui.components.Chip
import kz.mybrain.superkassa.desktop.ui.components.DetailLine
import kz.mybrain.superkassa.desktop.ui.components.EmptyState
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import kz.mybrain.superkassa.desktop.ui.theme.StatusColors

/**
 * Техническое состояние: что о кассе знают все, кто о ней знает.
 *
 * Прежде здесь стоял только снимок ОФД. Но о кассе говорят трое, и каждый
 * со своего места: узел — из своей базы, кабинет — из учёта КГД, ОФД —
 * из своего снимка. Сверки между ними не было ни в одном экране,
 * и расхождения выяснялись случайно: снятая с учёта касса встречала
 * кассира надписью «Активна».
 *
 * Показания стоят здесь же, плашками: отдельная карточка со своей
 * таблицей повторяла бы то, что эта уже показывает. Расходящееся
 * покрашено ролью отказа — видно не «где-то что-то не так», а кто именно
 * с кем не согласен. Источник, который о кассе не знает, плашки
 * не получает: незнание — не отрицание.
 *
 * Когда кассы у ОФД нет, это не «документов нет» — прежде здесь стояла
 * именно эта надпись, и владелец шёл искать пропавшие чеки. Касса просто
 * ещё ни разу не обращалась к ОФД, и так и сказано. Показания остальных
 * при этом всё равно видны: сверка нужнее всего тогда, когда ОФД молчит.
 */
@Composable
fun RegisterTechnical(
    state: RegisterState?,
    texts: CabinetTexts,
    claims: List<StateClaim>,
    disagreeing: Set<StateSource>
) {
    ClaimChips(texts.inWork, claims, disagreeing, texts) { it.usable }
    ClaimChips(texts.shift, claims, disagreeing, texts) { it.shift }
    val technical = state?.technicalState
    if (technical?.found != true) {
        EmptyState(AppIcons.kkm, texts.technicalUnknown, texts.technicalUnknownHint, dense = true)
        return
    }
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
        verticalArrangement = Arrangement.spacedBy(Spacing.tight)
    ) {
        TroubleChips(technical, texts)
    }
    DetailLine(texts.shift, technical.shiftNumber?.toString())
    DetailLine(texts.lastContact, cabinetMoment(technical.lastContactAt))
}

/**
 * Показания источников по одному вопросу.
 *
 * Строка не рисуется вовсе, когда о вопросе не знает никто: подпись
 * с пустотой под ней обещает сведения, которых нет.
 */
@Composable
private fun ClaimChips(
    question: String,
    claims: List<StateClaim>,
    disagreeing: Set<StateSource>,
    texts: CabinetTexts,
    answer: (StateClaim) -> Verdict
) {
    val spoken = claims.filter { answer(it) != Verdict.Unknown }
    if (spoken.isEmpty()) return
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
        verticalArrangement = Arrangement.spacedBy(Spacing.tight)
    ) {
        // Подпись вопроса стоит перед плашками, а не строкой выше:
        // без неё «ОФД · Нет» читается как отказ ОФД, а не как
        // закрытая смена.
        Text(
            text = question,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterVertically)
        )
        spoken.forEach { claim ->
            Chip(
                text = "${claim.source.title(texts)} · ${texts.word(answer(claim))}",
                color = when {
                    claim.source in disagreeing -> StatusColors.refused
                    answer(claim) == Verdict.Yes -> StatusColors.delivered
                    else -> StatusColors.pending
                }
            )
        }
    }
}

/** Ответ источника словами владельца. */
private fun CabinetTexts.word(verdict: Verdict): String = when (verdict) {
    Verdict.Yes -> verdictYes
    Verdict.No -> verdictNo
    Verdict.Unknown -> verdictUnknown
}

/** Помехи, из-за которых документы перестают доезжать до ОФД. */
@Composable
private fun TroubleChips(technical: TechnicalState, texts: CabinetTexts) {
    if (technical.trafficSuspended == true) {
        Chip(texts.trafficSuspended, StatusColors.refused)
    }
    if (technical.ofdDisconnected == true) {
        Chip(texts.ofdDisconnected, StatusColors.pending)
    }
}
