package kz.mybrain.superkassa.presentation.setup.step

import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.domain.setup.model.KkmSetupDraft
import kz.mybrain.superkassa.presentation.common.cabinet.CabinetSteps
import kz.mybrain.superkassa.presentation.setup.SetupActions
import kz.mybrain.superkassa.presentation.setup.SetupOffice
import kz.mybrain.superkassa.presentation.setup.component.StepDone
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Шаг кассы в кабинете БФД: войти по ЭЦП и завести кассу.
 *
 * Заводской номер в форму не вводится — он взят с прошлого шага,
 * и ошибиться в нём негде. Форма — та же, что в разделе точек кабинета:
 * своя копия однажды разошлась бы с ней. Без торговой точки кассу
 * не завести; точка заводится из той же формы, кнопкой под выбором точки.
 */
@Composable
internal fun CabinetStep(office: SetupOffice, draft: KkmSetupDraft, actions: SetupActions) {
    val setup = textsOf(LocalLanguage.current).setup
    val identifier = LocalStrings.current.settingsScreen.kkmIdentifier
    when {
        // Названная касса показана по имени, а идентификатор — строкой под ним.
        draft.cabinetRegisterId != null -> StepDone(
            what = setup.addedToCabinet,
            value = draft.name ?: draft.systemId,
            note = draft.systemId?.takeIf { draft.name != null }?.let { "$identifier $it" },
            doneLabel = setup.done
        )
        !office.session.open -> SignInFirst(office.cabinet)
        else -> AddRegister(office.cabinet, draft, actions)
    }
}

/** Заведение кассы в кабинете: окно формы раздела точек поверх шага. */
@Composable
private fun AddRegister(cabinet: CabinetSteps, draft: KkmSetupDraft, actions: SetupActions) {
    var adding by remember { mutableStateOf(false) }
    FilledTonalButton(onClick = { adding = true }) { Text(textsOf(LocalLanguage.current).cabinet.enroll.add) }
    if (!adding) return
    cabinet.AddRegister(
        factoryNumber = draft.factoryNumber.orEmpty(),
        year = draft.manufactureYear.orEmpty(),
        onDismiss = { adding = false }
    ) { created -> actions.rememberRegister(created.id, created.kkmId, created.internalName) }
}
