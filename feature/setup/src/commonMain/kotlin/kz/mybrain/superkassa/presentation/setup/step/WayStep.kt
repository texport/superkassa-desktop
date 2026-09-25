package kz.mybrain.superkassa.presentation.setup.step

import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.designsystem.picker.RadioRows
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.domain.setup.model.SetupWay
import kz.mybrain.superkassa.presentation.setup.SetupActions
import kz.mybrain.superkassa.presentation.setup.SetupUiState
import kz.mybrain.superkassa.strings.api.setup.SetupTexts
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Шаг выбора пути: через кабинет или вручную.
 *
 * Прежде путь выбирали переключателем над всеми шагами сразу, и ручной
 * путь от пути через кабинет отличался только набором карточек под ним.
 * Теперь это вопрос с двумя ответами, и у каждого — кто это делает и что
 * для этого нужно.
 */
@Composable
internal fun WayStep(state: SetupUiState, actions: SetupActions) {
    val setup = textsOf(LocalLanguage.current).setup
    RadioRows(
        options = SetupWay.entries,
        selected = state.way,
        title = { it.title(setup) },
        hint = { it.hint(setup) },
        onSelect = actions::chooseWay
    )
}

private fun SetupWay.title(texts: SetupTexts): String = when (this) {
    SetupWay.ViaCabinet -> texts.viaCabinet
    SetupWay.ByHand -> texts.manually
}

private fun SetupWay.hint(texts: SetupTexts): String = when (this) {
    SetupWay.ViaCabinet -> texts.viaCabinetHint
    SetupWay.ByHand -> texts.manuallyHint
}
