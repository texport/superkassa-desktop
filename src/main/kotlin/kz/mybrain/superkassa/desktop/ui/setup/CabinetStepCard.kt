package kz.mybrain.superkassa.desktop.ui.setup

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.KkmSetupDraft
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.cabinet.retailPlaces
import kz.mybrain.superkassa.desktop.ui.cabinet.AddPlaceCard
import kz.mybrain.superkassa.desktop.ui.cabinet.AddRegisterCard
import kz.mybrain.superkassa.desktop.ui.cabinet.FactoryStamp
import kz.mybrain.superkassa.desktop.ui.components.BusyButton
import kz.mybrain.superkassa.desktop.ui.strings.SetupTexts
import kz.mybrain.superkassa.desktop.ui.strings.cabinetTexts

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
        summary = listOfNotNull(setup.addedToCabinet, draft.systemId).joinToString(" · ")
    ) {
        if (draft.cabinetRegisterId != null) return@SetupStepCard
        if (!cabinet.open) {
            Text(setup.signInFirst)
            BusyButton(
                text = if (cabinet.busy) texts.signing else texts.signIn,
                busy = cabinet.busy,
                onClick = { scope.launch { cabinet.signIn() } }
            )
            return@SetupStepCard
        }
        // Без торговой точки кассу не завести, а у владельца, который
        // только начал, точек нет ни одной. Прежде мастер показывал пустой
        // список и упирался: точку заводили в другом разделе и возвращались.
        if (places == 0) {
            AddPlaceCard(session, cabinet, texts, opened = true) { scope.launch { countPlaces() } }
            return@SetupStepCard
        }
        AddRegisterCard(
            session = session,
            cabinet = cabinet,
            texts = texts,
            known = FactoryStamp(draft.factoryNumber.orEmpty(), draft.manufactureYear.orEmpty())
        ) { created -> draft.rememberRegister(created.id, created.kkmId) }
    }
}
