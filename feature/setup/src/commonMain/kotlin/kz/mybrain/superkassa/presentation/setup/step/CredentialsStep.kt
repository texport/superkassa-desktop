package kz.mybrain.superkassa.presentation.setup.step

import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.presentation.setup.SetupActions
import kz.mybrain.superkassa.presentation.setup.SetupUiState
import kz.mybrain.superkassa.presentation.setup.component.DigitsField

/**
 * Шаг ручного пути: идентификатор кассы и токен, выданные в БФД.
 *
 * Прежде они стояли в одной строке с контуром, пином, повтором пина
 * и кнопкой. Теперь здесь только то, что выдала БФД; контур и пин —
 * на следующем шаге, общем с путём через кабинет.
 */
@Composable
internal fun CredentialsStep(state: SetupUiState, actions: SetupActions) {
    val texts = LocalStrings.current.settingsScreen
    val form = state.byHand
    DigitsField(form.systemId, texts.kkmIdentifier) { actions.edit(form.copy(systemId = it)) }
    DigitsField(form.token, texts.token) { actions.edit(form.copy(token = it)) }
}
