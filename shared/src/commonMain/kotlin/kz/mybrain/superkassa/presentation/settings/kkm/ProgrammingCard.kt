package kz.mybrain.superkassa.presentation.settings.kkm

import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.presentation.common.section.SectionCard
import kz.mybrain.superkassa.presentation.common.status.Chip
import kz.mybrain.superkassa.presentation.settings.title
import kz.mybrain.superkassa.presentation.strings.common.LocalStrings
import kz.mybrain.superkassa.presentation.theme.StatusColors

/**
 * Режим программирования: в нём ли касса и чем это изменить.
 *
 * Касса принимает свои настройки и снятие только в этом режиме. Прежде
 * войти в него предлагала карточка, поля которой без него заперты, а выйти —
 * кнопка в диагностике, двумя разделами ниже: кассир входил и не находил,
 * чем выйти. Состояние и оба действия стоят здесь, под самой кассой,
 * и кнопка одна — названа по тому, что произойдёт.
 *
 * Плашка появляется только во включённом режиме: обычное состояние
 * называть незачем, а отличающееся кассир обязан видеть с одного взгляда.
 */
@Composable
internal fun ProgrammingCard(kkm: KkmSettingsUiState, actions: KkmSettingsActions) {
    val texts = LocalStrings.current.settings
    kkm.kkm ?: return
    val inside = kkm.programming
    SectionCard(
        title = texts.programmingMode,
        info = texts.programmingRequired,
        trailing = { if (inside) Chip(texts.programmingOn, StatusColors.pending) }
    ) {
        FilledTonalButton(enabled = !kkm.busy, onClick = actions::switchProgramming) {
            Text(if (inside) texts.exitProgramming else texts.enterProgramming)
        }
    }
}
