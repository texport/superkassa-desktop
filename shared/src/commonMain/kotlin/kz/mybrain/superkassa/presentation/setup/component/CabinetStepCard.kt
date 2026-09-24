package kz.mybrain.superkassa.presentation.setup.component

import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.domain.setup.model.KkmSetupDraft
import kz.mybrain.superkassa.presentation.cabinet.CabinetUiState
import kz.mybrain.superkassa.presentation.cabinet.CabinetWindow
import kz.mybrain.superkassa.presentation.cabinet.enroll.AddRegisterDialog
import kz.mybrain.superkassa.presentation.cabinet.enroll.FactoryStamp
import kz.mybrain.superkassa.presentation.cabinet.signin.SignInAction
import kz.mybrain.superkassa.presentation.common.model.collectAsScreenState
import kz.mybrain.superkassa.presentation.common.strings.LocalLanguage
import kz.mybrain.superkassa.presentation.theme.icon.Glyphs
import kz.mybrain.superkassa.strings.api.cabinet.CabinetTexts
import kz.mybrain.superkassa.strings.api.setup.SetupTexts
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Шаг 2: касса заводится в кабинете ОФД.
 *
 * Вход по ЭЦП здесь же: владелец не должен искать другой экран, чтобы
 * продолжить начатое. Заводской номер в форму не вводится — он взят
 * из первого шага, и ошибиться в нём негде.
 *
 * Формы — те же, что в разделах кабинета: заведение точки и заведение
 * кассы. Своя копия каждой означала бы, что однажды поправят только одну.
 */
@Composable
fun CabinetStepCard(
    cabinet: CabinetWindow,
    setup: SetupTexts,
    draft: KkmSetupDraft,
    onRegister: (id: String, kkmId: Int, name: String?) -> Unit
) {
    // Точки мастер не читает: форма заведения кассы ищет точку у кабинета
    // сама, одной страницей, — обход всей сети на пути мастера не нужен.
    val window by cabinet.cabinet.state.collectAsScreenState()

    SetupStepCard(
        title = setup.stepCabinet,
        hint = setup.stepCabinetHint,
        texts = setup,
        done = draft.cabinetRegisterId != null,
        ready = draft.factoryNumber != null,
        summary = listOfNotNull(setup.addedToCabinet, draft.systemId).joinToString(Glyphs.SEPARATOR)
    ) {
        if (draft.cabinetRegisterId == null) CabinetStep(cabinet, window, setup, draft, onRegister)
    }
}

/**
 * Что делать в кабинете сейчас: войти или завести кассу.
 *
 * Без торговой точки кассу не завести, а у владельца, который только начал,
 * точек нет ни одной. Точка заводится из той же формы кассы — кнопкой под
 * выбором точки, — поэтому мастер не читает точки сети, чтобы решить,
 * какую кнопку показать: это был обход всех страниц сети на пути мастера.
 */
@Composable
private fun CabinetStep(
    cabinet: CabinetWindow,
    window: CabinetUiState,
    setup: SetupTexts,
    draft: KkmSetupDraft,
    onRegister: (id: String, kkmId: Int, name: String?) -> Unit
) {
    val language = LocalLanguage.current
    val texts = textsOf(language).cabinet
    when {
        !window.open -> {
            Text(setup.signInFirst)
            SignInAction(cabinet = cabinet.cabinet, language = language, texts = texts, modifier = Modifier)
        }

        else -> AddRegisterStep(
            cabinet = cabinet,
            texts = texts,
            known = FactoryStamp(draft.factoryNumber.orEmpty(), draft.manufactureYear.orEmpty())
        ) { created -> onRegister(created.id, created.kkmId, created.internalName) }
    }
}

/** Создание кассы в мастере: то же окно, что и в разделе точек. */
@Composable
private fun AddRegisterStep(
    cabinet: CabinetWindow,
    texts: CabinetTexts,
    known: FactoryStamp,
    onAdded: (CabinetRegister) -> Unit
) {
    var adding by remember { mutableStateOf(false) }
    FilledTonalButton(onClick = { adding = true }) { Text(texts.addRegister) }
    if (adding) {
        AddRegisterDialog(cabinet, texts, known, onDismiss = { adding = false }, onAdded = onAdded)
    }
}
