package kz.mybrain.superkassa.presentation.settings.kkm

import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.designsystem.picker.SwitchRow
import kz.mybrain.superkassa.designsystem.section.SettingGroup
import kz.mybrain.superkassa.designsystem.status.Chip
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.designsystem.theme.StatusColors

/**
 * Режим программирования: в нём ли касса, и переключатель, который это меняет.
 *
 * Касса принимает свои настройки и снятие только в этом режиме. Прежде
 * войти в него предлагала карточка, поля которой без него заперты, а выйти —
 * кнопка в диагностике, двумя разделами ниже: кассир входил и не находил,
 * чем выйти. Состояние и его смена стоят здесь, под самой кассой, одной
 * строкой-переключателем Material 3: положение переключателя и есть
 * состояние кассы.
 *
 * Плашка появляется только во включённом режиме: обычное состояние
 * называть незачем, а отличающееся кассир обязан видеть с одного взгляда.
 */
@Composable
internal fun ProgrammingCard(kkm: KkmSettingsUiState, actions: KkmSettingsActions) {
    val texts = LocalStrings.current.settingsScreen
    kkm.kkm ?: return
    val inside = kkm.programming
    SettingGroup(
        title = texts.programmingMode,
        trailing = { if (inside) Chip(texts.programmingOn, StatusColors.pending) }
    ) {
        SwitchRow(
            title = if (inside) texts.exitProgramming else texts.enterProgramming,
            checked = inside,
            onSwitch = { actions.switchProgramming() },
            enabled = !kkm.busy,
            hint = texts.programmingRequired
        )
    }
}
