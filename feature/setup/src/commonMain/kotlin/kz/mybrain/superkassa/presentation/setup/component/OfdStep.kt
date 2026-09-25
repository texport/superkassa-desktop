package kz.mybrain.superkassa.presentation.setup.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.button.FieldButton
import kz.mybrain.superkassa.designsystem.button.FieldButtonKind
import kz.mybrain.superkassa.designsystem.field.fieldWidth
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.designsystem.theme.size.Sizes
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.presentation.setup.SetupActions
import kz.mybrain.superkassa.presentation.setup.SetupUiState

/**
 * Идентификатор и токен, выданные БФД.
 *
 * Список контуров приходит от кассы: свой в приложении означал бы, что
 * нового контура владелец не увидит, пока не обновит программу.
 *
 * Своей карточки шаг не рисует — её ставит мастер: прежде он рисовал
 * заголовок снаружи, и рядом с карточкой первого шага это выглядело
 * как куски из разных экранов.
 */
@Composable
internal fun OfdStep(state: SetupUiState, actions: SetupActions) {
    val texts = LocalStrings.current.settings
    val form = state.byHand
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.fieldGap),
        verticalArrangement = Arrangement.spacedBy(Spacing.itemGap),
        itemVerticalAlignment = Alignment.Top
    ) {
        ContourPicker(state.contours, state.contour) { actions.edit(form.copy(contour = it)) }
        DigitsField(form.systemId, texts.kkmIdentifier) { actions.edit(form.copy(systemId = it)) }
        DigitsField(form.token, texts.token) { actions.edit(form.copy(token = it)) }
        AdminPinField(form.adminPin, Modifier.fieldWidth(texts.adminPin, Sizes.fieldChoice)) {
            actions.edit(form.copy(adminPin = it))
        }
        RepeatPinField(form, Modifier.fieldWidth(texts.adminPin, Sizes.fieldChoice)) {
            actions.edit(form.copy(adminPinRepeat = it))
        }
        // Действие стоит в строке с полями и того же роста, что они.
        FieldButton(
            text = if (form.busy) texts.registering else texts.register,
            kind = FieldButtonKind.Filled,
            enabled = state.ready
        ) { actions.connect {} }
    }
}

/** Число, выданное БФД: идентификатор кассы или токен. Лишнее модель отбрасывает сама. */
@Composable
private fun DigitsField(value: String, label: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fieldWidth(label, Sizes.fieldChoice)
    )
}
