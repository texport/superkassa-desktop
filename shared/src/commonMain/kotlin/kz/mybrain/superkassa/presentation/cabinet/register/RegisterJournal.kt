package kz.mybrain.superkassa.presentation.cabinet.register

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.designsystem.format.Dates
import kz.mybrain.superkassa.designsystem.list.RecordRow
import kz.mybrain.superkassa.designsystem.list.stripedAt
import kz.mybrain.superkassa.designsystem.state.EmptyState
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.designsystem.theme.icon.Glyphs
import kz.mybrain.superkassa.domain.cabinet.model.documents.RegistrationAction
import kz.mybrain.superkassa.presentation.cabinet.CabinetStatusChip
import kz.mybrain.superkassa.presentation.cabinet.actionTitle
import kz.mybrain.superkassa.strings.api.cabinet.CabinetTexts

/**
 * Журнал регистрационных действий кассы.
 *
 * Что подано, когда и чем закончилось. Причина отказа стоит в служебной
 * строке рядом со временем: она приходит от ИСНА и объясняет, почему
 * заявление придётся подавать заново.
 */
@Composable
fun RegisterJournal(actions: List<RegistrationAction>, texts: CabinetTexts) {
    if (actions.isEmpty()) {
        EmptyState(AppIcons.history, texts.actionsEmpty, texts.hints.actionsEmpty)
        return
    }
    actions.forEachIndexed { at, action ->
        RecordRow(
            title = actionTitle(action.actionType, texts),
            subtitle = listOfNotNull(
                Dates.momentOf(action.createdAt),
                action.registrationNumber,
                action.reasonMessage
            ).joinToString(Glyphs.SEPARATOR),
            striped = stripedAt(at),
            trailing = { CabinetStatusChip(action.status, texts) }
        )
    }
}

/** Сколько действий в журнале — видно и свёрнутым. */
@Composable
fun ActionsCount(count: Int) {
    Text(text = count.toString(), style = MaterialTheme.typography.labelLarge)
}
