package kz.mybrain.superkassa.presentation.settings.kkm

import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.designsystem.picker.SwitchRow
import kz.mybrain.superkassa.designsystem.section.SettingGroup
import kz.mybrain.superkassa.designsystem.status.Chip
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.designsystem.theme.StatusColors
import kz.mybrain.superkassa.strings.api.textsOf

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
            // Подпись называет состояние и не меняется от положения
            // переключателя (Material 3, Switch): «Выйти из программирования»
            // у включённого читалось как кнопка, а не как положение.
            title = textsOf(LocalLanguage.current).kassa.money.kkm.needProgramming,
            checked = inside,
            onSwitch = { actions.switchProgramming() },
            enabled = !kkm.busy,
            hint = texts.programmingRequired
        )
    }
}
