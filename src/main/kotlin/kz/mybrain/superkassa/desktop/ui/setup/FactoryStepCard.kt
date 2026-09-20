package kz.mybrain.superkassa.desktop.ui.setup

import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.KkmSetupDraft
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.factoryInfo
import kz.mybrain.superkassa.desktop.ui.strings.SetupTexts
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs

/**
 * Шаг 1: заводской номер кассы.
 *
 * Узел считает номер по алгоритму производителя и выдаёт новый на каждый
 * запрос. Раньше экран показывал его и забывал: владелец, открывший экран
 * дважды, уносил в кабинет один номер, а видел потом другой. Теперь номер
 * запоминается черновиком и берётся оттуда до конца подключения.
 */
@Composable
fun FactoryStepCard(session: Session, setup: SetupTexts, draft: KkmSetupDraft) {
    val scope = rememberCoroutineScope()
    val number = draft.factoryNumber

    SetupStepCard(
        title = setup.stepFactory,
        hint = setup.stepFactoryHint,
        texts = setup,
        done = number != null,
        ready = true,
        summary = listOfNotNull(number, draft.manufactureYear).joinToString(Glyphs.SEPARATOR)
    ) {
        if (number != null) return@SetupStepCard
        FilledTonalButton(
            enabled = !session.busy,
            onClick = {
                scope.launch {
                    val info = session.guard(setup.stepFactory) { session.client.factoryInfo() }
                        ?: return@launch
                    draft.rememberFactory(info.factoryNumber, info.manufactureYear.toString())
                }
            }
        ) { Text(setup.getFactory) }
    }
}
