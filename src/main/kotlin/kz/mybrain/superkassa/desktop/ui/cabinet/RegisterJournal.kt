package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.desktop.server.cabinet.RegistrationAction
import kz.mybrain.superkassa.desktop.ui.components.EmptyState
import kz.mybrain.superkassa.desktop.ui.components.RecordRow
import kz.mybrain.superkassa.desktop.ui.components.stripedAt
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs

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
                cabinetMoment(action.createdAt),
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
