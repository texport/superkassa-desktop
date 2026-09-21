package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.app.enrollKkm
import kz.mybrain.superkassa.desktop.app.workOn
import kz.mybrain.superkassa.desktop.server.Dictionary
import kz.mybrain.superkassa.desktop.server.KkmInitRequest
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetRegister
import kz.mybrain.superkassa.desktop.server.cabinet.RegisterState
import kz.mybrain.superkassa.desktop.server.cabinet.issueToken
import kz.mybrain.superkassa.desktop.ui.components.FormDialog
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.MachineTexts
import kz.mybrain.superkassa.desktop.ui.strings.machineTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons

/**
 * Заведение кассы кабинета на этой машине — одним действием владельца.
 *
 * Ход: токен, заведение на узле, переход к кассе. Владельцу остаётся
 * выбрать контур БФД и назначить пин администратора; идентификатор кассы
 * у БФД берётся из карточки кабинета, а не переписывается руками.
 *
 * Предупреждение о перевыпуске токена стоит постоянной строкой, а не под
 * значком: это последствие действия, а не справка о нём. Если БФД кассу
 * слышит, предупреждения мало — нужна отдельная отметка владельца.
 */
@Composable
fun AdoptRegisterDialog(
    session: Session,
    cabinet: CabinetSession,
    texts: CabinetTexts,
    register: CabinetRegister,
    state: RegisterState?,
    onDismiss: () -> Unit
) {
    val settings = LocalStrings.current.settings
    val machine = machineTexts(session.language)
    val scope = rememberCoroutineScope()
    val draft = remember(register.id) { AdoptDraft(session.preferences) }
    val environments = session.dictionaries[Dictionary.OfdEnvironments].orEmpty()
    val handoverNeeded = heardElsewhere(state?.technicalState)
    val labels = AdoptLabels(settings.ofd, settings.adminPin, machine.handoverUnderstood)

    // Справочник приходит с узла позже первой отрисовки: подстановка
    // делается эффектом, иначе окно осталось бы с пустым выбором контура.
    LaunchedEffect(environments) { draft.preset(environments) }

    FormDialog(
        title = machine.workHere,
        icon = AppIcons.newKkm,
        action = if (draft.stranded) machine.retry else machine.workHere,
        close = texts.close,
        busy = session.busy || cabinet.busy,
        missing = adoptMissing(draft.form(handoverNeeded), labels),
        onDismiss = onDismiss,
        onAction = { scope.launch { adopt(session, cabinet, machine, register, draft, onDismiss) } }
    ) {
        AdoptFields(session, texts, draft, environments, state?.technicalState)
    }
}

/**
 * Ход действия целиком.
 *
 * Токен выпускается один раз за окно: если заведение сорвалось после
 * выпуска, прежний токен кассы уже мёртв, и второй выпуск убил бы ещё
 * и этот. Повтор шлёт узлу тот же токен, который держит [AdoptDraft].
 */
private suspend fun adopt(
    session: Session,
    cabinet: CabinetSession,
    machine: MachineTexts,
    register: CabinetRegister,
    draft: AdoptDraft,
    onDone: () -> Unit
) {
    draft.retrying()
    val token = draft.issued ?: reissueToken(cabinet, register) ?: return
    draft.keepToken(token)
    val request = KkmInitRequest(
        ofdId = draft.target.provider,
        ofdEnvironment = draft.target.environment,
        ofdSystemId = register.kkmId.toString(),
        ofdToken = token,
        adminPin = draft.adminPin
    )
    val kkm = session.enrollKkm(request, machine.workHere, register.internalName)
    if (kkm == null) {
        draft.strand()
        return
    }
    draft.remember()
    session.workOn(kkm)
    // Последнее слово владельцу — что делать дальше, а не состояние кассы:
    // сообщение в окне одно, и оно должно вести к работе.
    session.report(machine.done)
    onDone()
}

/** Перевыпуск технического токена: действующий кабинет показать не умеет. */
private suspend fun reissueToken(cabinet: CabinetSession, register: CabinetRegister): String? {
    val access = cabinet.token ?: return null
    return cabinet.guard { cabinet.client.issueToken(access, register.id) }?.token?.toString()
}
