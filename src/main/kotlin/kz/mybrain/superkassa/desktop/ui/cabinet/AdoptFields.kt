package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.DictionaryEntry
import kz.mybrain.superkassa.desktop.server.cabinet.TechnicalState
import kz.mybrain.superkassa.desktop.ui.components.DetailLine
import kz.mybrain.superkassa.desktop.ui.components.OfdChoice
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.MachineTexts
import kz.mybrain.superkassa.desktop.ui.strings.machineTexts
import kz.mybrain.superkassa.desktop.ui.strings.moneyTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import kz.mybrain.superkassa.desktop.ui.users.UserRules
import kz.mybrain.superkassa.desktop.ui.users.pinProblem

/**
 * Что заполняет владелец, заводя кассу на этой машине.
 *
 * Порядок сверху вниз: чем касса будет работать, кто в неё войдёт и чем
 * это действие обойдётся. Последствие стоит последним и на виду — оно
 * читается перед нажатием, а не после.
 */
@Composable
internal fun AdoptFields(
    session: Session,
    texts: CabinetTexts,
    draft: AdoptDraft,
    environments: List<DictionaryEntry>,
    technical: TechnicalState?
) {
    val machine = machineTexts(session.language)
    OfdChoice(draft.target, environments, session.language.code) { draft.target = it }
    AdminPinField(session, draft)
    WarningRow(machine.tokenReissued)
    if (heardElsewhere(technical)) {
        HandoverConsent(texts, machine, draft, technical)
    }
    if (draft.stranded) {
        WarningRow(machine.stranded)
    }
}

/**
 * Пин администратора будущей кассы.
 *
 * Проверяется теми же правилами, что и пин кассира: узел откажет по длине
 * и по стандартному пину, и владелец должен увидеть причину до нажатия,
 * а не отказом узла после.
 */
@Composable
private fun AdminPinField(session: Session, draft: AdoptDraft) {
    val strings = LocalStrings.current
    val cashiers = moneyTexts(session.language).cashiers
    val trouble = pinProblem(draft.adminPin, cashiers, strings.users.forbiddenPin)
    OutlinedTextField(
        value = draft.adminPin,
        onValueChange = { draft.adminPin = UserRules.digitsOf(it) },
        label = { Text(strings.settings.adminPin) },
        singleLine = true,
        isError = trouble != null,
        placeholder = { Text(cashiers.pinLength) },
        supportingText = trouble?.let { { Text(it) } },
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * Отдельная отметка о том, что касса замолчит на другой машине.
 *
 * Здесь названо и когда именно БФД её слышала: «недавно» владелец
 * истолкует как угодно, а дата и час говорят сами за себя.
 */
@Composable
private fun HandoverConsent(
    texts: CabinetTexts,
    machine: MachineTexts,
    draft: AdoptDraft,
    technical: TechnicalState?
) {
    DetailLine(machine.heardByOfd, cabinetMoment(technical?.lastContactAt))
    DetailLine(texts.shift, technical?.shiftNumber?.toString())
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = draft.handoverAccepted,
            onCheckedChange = { draft.handoverAccepted = it }
        )
        Text(text = machine.handoverUnderstood, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Последствие действия: значок и строка цветом отказа, а не подсказка под значком. */
@Composable
private fun WarningRow(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = AppIcons.warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
    }
}
