package kz.mybrain.superkassa.presentation.setup.step

import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.designsystem.button.BusyButton
import kz.mybrain.superkassa.designsystem.button.FieldButtonKind
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.presentation.setup.SetupActions
import kz.mybrain.superkassa.presentation.setup.SetupUiState
import kz.mybrain.superkassa.presentation.setup.component.StepDone
import kz.mybrain.superkassa.strings.api.fill
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Шаг заводского номера.
 *
 * Касса считает номер по алгоритму производителя и выдаёт новый на каждый
 * запрос. Раньше экран показывал его и забывал: владелец, открывший экран
 * дважды, уносил в кабинет один номер, а видел потом другой. Теперь номер
 * запоминается, и полученный показан крупно — его переписывают и диктуют.
 * Кнопка получения — тональная: главное действие шага — «Далее».
 */
@Composable
internal fun FactoryStep(state: SetupUiState, actions: SetupActions) {
    val setup = textsOf(LocalLanguage.current).setup
    val number = state.draft.factoryNumber
    if (number == null) {
        BusyButton(
            text = setup.getFactory,
            busy = state.gettingFactory,
            kind = FieldButtonKind.Tonal,
            onClick = actions::getFactory
        )
    } else {
        val year = state.draft.manufactureYear?.let { setup.factoryYear.fill(it) }
        StepDone(what = null, value = number, note = year, doneLabel = setup.done)
    }
}
