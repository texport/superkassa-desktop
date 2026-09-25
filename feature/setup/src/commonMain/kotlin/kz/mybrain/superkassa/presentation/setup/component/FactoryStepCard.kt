package kz.mybrain.superkassa.presentation.setup.component

import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.designsystem.theme.icon.Glyphs
import kz.mybrain.superkassa.presentation.setup.SetupActions
import kz.mybrain.superkassa.presentation.setup.SetupUiState
import kz.mybrain.superkassa.strings.api.setup.SetupTexts

/**
 * Шаг 1: заводской номер кассы.
 *
 * Касса считает номер по алгоритму производителя и выдаёт новый на каждый
 * запрос. Раньше экран показывал его и забывал: владелец, открывший экран
 * дважды, уносил в кабинет один номер, а видел потом другой. Теперь номер
 * запоминается и берётся из пройденного до конца подключения.
 */
@Composable
internal fun FactoryStepCard(state: SetupUiState, actions: SetupActions, setup: SetupTexts) {
    val number = state.draft.factoryNumber
    SetupStepCard(
        title = setup.stepFactory,
        hint = setup.stepFactoryHint,
        texts = setup,
        done = number != null,
        ready = true,
        summary = listOfNotNull(number, state.draft.manufactureYear).joinToString(Glyphs.SEPARATOR)
    ) {
        if (number != null) return@SetupStepCard
        FilledTonalButton(enabled = !state.gettingFactory, onClick = actions::getFactory) { Text(setup.getFactory) }
    }
}
