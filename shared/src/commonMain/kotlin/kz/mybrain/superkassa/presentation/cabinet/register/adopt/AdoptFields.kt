package kz.mybrain.superkassa.presentation.cabinet.register.adopt

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
import io.github.texport.superkassa.core.presentation.api.model.reference.OfdEnvironmentResponse
import kz.mybrain.superkassa.domain.cabinet.model.documents.TechnicalState
import kz.mybrain.superkassa.domain.setup.model.OfdContours
import kz.mybrain.superkassa.domain.users.model.UserRules
import kz.mybrain.superkassa.presentation.cabinet.register.RegisterView
import kz.mybrain.superkassa.presentation.cabinet.register.heardElsewhere
import kz.mybrain.superkassa.presentation.common.format.Dates
import kz.mybrain.superkassa.presentation.common.picker.LabelledPicker
import kz.mybrain.superkassa.presentation.common.section.DetailLine
import kz.mybrain.superkassa.presentation.strings.cabinet.CabinetTexts
import kz.mybrain.superkassa.presentation.strings.cabinet.MachineTexts
import kz.mybrain.superkassa.presentation.strings.cabinet.machineTexts
import kz.mybrain.superkassa.presentation.strings.common.Language
import kz.mybrain.superkassa.presentation.strings.common.LocalLanguage
import kz.mybrain.superkassa.presentation.strings.common.LocalStrings
import kz.mybrain.superkassa.presentation.strings.kassa.moneyTexts
import kz.mybrain.superkassa.presentation.theme.icon.AppIcons
import kz.mybrain.superkassa.presentation.theme.size.Sizes
import kz.mybrain.superkassa.presentation.theme.size.Spacing
import kz.mybrain.superkassa.presentation.users.pinProblem

/**
 * Что заполняет владелец, заводя кассу на этой машине.
 *
 * Порядок сверху вниз: чем касса будет работать, кто в неё войдёт и чем
 * это действие обойдётся. Последствие стоит последним и на виду — оно
 * читается перед нажатием, а не после.
 */
@Composable
internal fun AdoptFields(
    texts: CabinetTexts,
    draft: AdoptDraft,
    adopt: AdoptUiState,
    view: RegisterView
) {
    val language = LocalLanguage.current
    val machine = machineTexts(language)
    val technical = view.state.state?.technicalState
    val stranded = view.card.id in adopt.stranded
    EnvironmentChoice(draft, adopt.environments, language)
    AdminPinField(draft)
    WarningRow(machine.tokenReissued)
    if (heardElsewhere(technical)) {
        HandoverConsent(texts, machine, draft, technical)
    }
    if (stranded) {
        WarningRow(machine.stranded)
    }
}

/**
 * Контур БФД из справочника кассы. Неподнятый контур гаснет по общему
 * правилу — см. `OfdContours.raised`.
 */
@Composable
private fun EnvironmentChoice(draft: AdoptDraft, environments: List<OfdEnvironmentResponse>, language: Language) {
    LabelledPicker(
        label = LocalStrings.current.settings.environment,
        options = environments,
        selected = environments.firstOrNull { it.code == draft.target.environment },
        title = { entry ->
            entry?.name?.let { name ->
                when (language) {
                    Language.Ru -> name.ru
                    Language.Kk -> name.kk
                    Language.En -> name.en
                }
            } ?: draft.target.environment
        },
        onSelect = { draft.target = draft.target.copy(environment = it.code) },
        available = { OfdContours.raised(it.code) },
        width = Sizes.fieldChoice
    )
}

/**
 * Пин администратора будущей кассы.
 *
 * Проверяется теми же правилами, что и пин кассира: касса откажет пину
 * не той длины, и владелец должен увидеть причину до нажатия, а не отказом
 * кассы после.
 */
@Composable
private fun AdminPinField(draft: AdoptDraft) {
    val language = LocalLanguage.current
    val strings = LocalStrings.current
    val cashiers = moneyTexts(language).cashiers
    val trouble = pinProblem(draft.adminPin, cashiers)
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
    DetailLine(machine.heardByOfd, Dates.momentOf(technical?.lastContactAt))
    DetailLine(texts.shift, technical?.shiftNumber?.toString())
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.buttonGap),
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
        horizontalArrangement = Arrangement.spacedBy(Spacing.buttonGap),
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
