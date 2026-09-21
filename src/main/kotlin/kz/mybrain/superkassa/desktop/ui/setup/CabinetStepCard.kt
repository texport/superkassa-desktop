package kz.mybrain.superkassa.desktop.ui.setup

import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.KkmSetupDraft
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetRegister
import kz.mybrain.superkassa.desktop.server.cabinet.retailPlaces
import kz.mybrain.superkassa.desktop.ui.cabinet.AddPlaceCard
import kz.mybrain.superkassa.desktop.ui.cabinet.AddRegisterDialog
import kz.mybrain.superkassa.desktop.ui.cabinet.FactoryStamp
import kz.mybrain.superkassa.desktop.ui.cabinet.SignInAction
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.strings.SetupTexts
import kz.mybrain.superkassa.desktop.ui.strings.cabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs

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
    session: Session,
    cabinet: CabinetSession,
    setup: SetupTexts,
    draft: KkmSetupDraft
) {
    val texts = cabinetTexts(session.language)
    val scope = rememberCoroutineScope()
    var places by remember { mutableStateOf(0) }

    suspend fun countPlaces() {
        val token = cabinet.token ?: return
        places = cabinet.guard { cabinet.client.retailPlaces(token) }?.items?.size ?: 0
    }

    LaunchedEffect(cabinet.token) { countPlaces() }

    SetupStepCard(
        title = setup.stepCabinet,
        hint = setup.stepCabinetHint,
        texts = setup,
        done = draft.cabinetRegisterId != null,
        ready = draft.factoryNumber != null,
        summary = listOfNotNull(setup.addedToCabinet, draft.systemId).joinToString(Glyphs.SEPARATOR)
    ) {
        if (draft.cabinetRegisterId != null) return@SetupStepCard
        if (!cabinet.open) {
            Text(setup.signInFirst)
            SignInAction(
                cabinet = cabinet,
                language = session.language,
                texts = texts,
                modifier = Modifier
            )
            return@SetupStepCard
        }
        // Без торговой точки кассу не завести, а у владельца, который
        // только начал, точек нет ни одной. Прежде мастер показывал пустой
        // список и упирался: точку заводили в другом разделе и возвращались.
        if (places == 0) {
            AddPlaceStep(session, cabinet, texts) { scope.launch { countPlaces() } }
            return@SetupStepCard
        }
        AddRegisterStep(
            session = session,
            cabinet = cabinet,
            texts = texts,
            known = FactoryStamp(draft.factoryNumber.orEmpty(), draft.manufactureYear.orEmpty())
        ) { created -> draft.rememberRegister(created.id, created.kkmId, created.internalName) }
    }
}

/**
 * Создание первой точки прямо в мастере.
 *
 * Форма открывается окном — тем же, что и в разделе кабинета: две формы
 * заведения точки разошлись бы на первой правке.
 */
@Composable
private fun AddPlaceStep(
    session: Session,
    cabinet: CabinetSession,
    texts: CabinetTexts,
    onAdded: () -> Unit
) {
    var adding by remember { mutableStateOf(false) }
    FilledTonalButton(onClick = { adding = true }) { Text(texts.addPlace) }
    if (adding) {
        AddPlaceCard(session, cabinet, texts, onDismiss = { adding = false }, onAdded = onAdded)
    }
}

/** Создание кассы в мастере: то же окно, что и в разделе точек. */
@Composable
private fun AddRegisterStep(
    session: Session,
    cabinet: CabinetSession,
    texts: CabinetTexts,
    known: FactoryStamp,
    onAdded: (CabinetRegister) -> Unit
) {
    var adding by remember { mutableStateOf(false) }
    FilledTonalButton(onClick = { adding = true }) { Text(texts.addRegister) }
    if (adding) {
        AddRegisterDialog(session, cabinet, texts, known, onDismiss = { adding = false }, onAdded = onAdded)
    }
}
