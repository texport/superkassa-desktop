package kz.mybrain.superkassa.presentation.cabinet.register.adopt

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.presentation.cabinet.CabinetWindow
import kz.mybrain.superkassa.presentation.cabinet.register.AdoptLabels
import kz.mybrain.superkassa.presentation.cabinet.register.RegisterView
import kz.mybrain.superkassa.presentation.cabinet.register.adoptMissing
import kz.mybrain.superkassa.presentation.cabinet.register.heardElsewhere
import kz.mybrain.superkassa.presentation.common.dialog.FormDialog
import kz.mybrain.superkassa.presentation.common.model.collectAsScreenState
import kz.mybrain.superkassa.presentation.common.strings.LocalLanguage
import kz.mybrain.superkassa.presentation.common.strings.LocalStrings
import kz.mybrain.superkassa.presentation.theme.icon.AppIcons
import kz.mybrain.superkassa.presentation.users.signin.LoginViewModel
import kz.mybrain.superkassa.presentation.users.signin.loginViewModel
import kz.mybrain.superkassa.strings.api.cabinet.CabinetTexts
import kz.mybrain.superkassa.strings.api.cabinet.MachineTexts
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Заведение кассы кабинета на этой машине — одним действием владельца.
 *
 * Ход: токен, заведение в кассе приложения, переход к кассе. Владельцу остаётся
 * выбрать контур БФД и назначить пин администратора; идентификатор кассы
 * у БФД берётся из карточки кабинета, а не переписывается руками.
 *
 * Предупреждение о перевыпуске токена стоит постоянной строкой, а не под
 * значком: это последствие действия, а не справка о нём. Если БФД кассу
 * слышит, предупреждения мало — нужна отдельная отметка владельца.
 */
@Composable
fun AdoptRegisterDialog(
    cabinet: CabinetWindow,
    texts: CabinetTexts,
    view: RegisterView,
    onDismiss: () -> Unit
) {
    val language = LocalLanguage.current
    val register = view.card
    val machine = textsOf(language).cabinet.machine
    val model = adoptViewModel(cabinet.cabinet)
    val adopt by model.state.collectAsScreenState()
    val login = loginViewModel(cabinet.app)
    val draft = remember(register.id) { AdoptDraft(cabinet.app.areas.setup?.memory) }
    // Справочник приходит позже первой отрисовки: подстановка делается
    // эффектом, иначе окно осталось бы с пустым выбором контура.
    LaunchedEffect(adopt.environments) { draft.preset(adopt.environments) }
    FormDialog(
        title = machine.workHere,
        icon = AppIcons.newKkm,
        action = if (register.id in adopt.stranded) machine.retry else machine.workHere,
        close = texts.close,
        busy = adopt.adopting,
        missing = adoptMissing(draft.form(heardElsewhere(view.state.state?.technicalState)), adoptLabels(machine)),
        onDismiss = onDismiss,
        onAction = { adopt(model, register, draft, login, onDismiss) }
    ) {
        AdoptFields(texts, draft, adopt, view)
    }
}

/**
 * Заводит кассу; заведена — контур запоминается рабочим местом, а новая касса
 * появляется на входе сразу: список касс входа перечитывается.
 */
private fun adopt(
    model: AdoptViewModel,
    register: CabinetRegister,
    draft: AdoptDraft,
    login: LoginViewModel,
    onDone: () -> Unit
) = model.adopt(register, draft.target.environment, draft.adminPin) {
    draft.remember()
    login.reload()
    onDone()
}

/** Подписи полей окна: они объявлены в наборах надписей, а не здесь. */
@Composable
private fun adoptLabels(machine: MachineTexts): AdoptLabels {
    val settings = LocalStrings.current.settings
    return AdoptLabels(settings.ofd, settings.adminPin, machine.handoverUnderstood)
}
