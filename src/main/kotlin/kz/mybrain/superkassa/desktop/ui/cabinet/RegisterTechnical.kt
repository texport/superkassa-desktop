package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
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
 * Техническое состояние: что о кассе знает сервер приёма данных.
 *
 * Когда кассы там нет, это не «документов нет» — прежде здесь стояла
 * именно эта надпись, и владелец шёл искать пропавшие чеки. Касса просто
 * ещё ни разу не обращалась к ОФД, и так и сказано.
 *
 * Приостановленный приём и разорванная связь вынесены плашками: они
 * приходили в ответе и раньше, но на экран не попадали — а это ровно то,
 * из-за чего чеки перестают доезжать.
 */
@Composable
fun RegisterTechnical(state: RegisterState?, texts: CabinetTexts) {
    val technical = state?.technicalState
    if (technical?.found != true) {
        EmptyState(AppIcons.kkm, texts.technicalUnknown, texts.technicalUnknownHint)
        return
    }
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
        verticalArrangement = Arrangement.spacedBy(Spacing.tight)
    ) {
        CabinetStatusChip(technical.shiftStatus, texts)
        TroubleChips(technical, texts)
    }
    DetailLine(texts.shift, technical.shiftNumber?.toString())
    DetailLine(texts.lastContact, cabinetMoment(technical.lastContactAt))
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
