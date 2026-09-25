package kz.mybrain.superkassa.presentation.setup.step

import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.domain.setup.model.SetupWay
import kz.mybrain.superkassa.presentation.setup.SetupParts
import kz.mybrain.superkassa.presentation.setup.component.AdminPinField
import kz.mybrain.superkassa.presentation.setup.component.ContourPicker
import kz.mybrain.superkassa.presentation.setup.component.RepeatPinField

/**
 * Последний шаг: контур и пин администратора, затем касса заводится.
 *
 * Пинов по умолчанию у кассы нет, и без пина касса не заводится. Через
 * кабинет токен выпускается в миг нажатия «Завести кассу» и в файл
 * не попадает; выпускает его кабинет, поэтому вход в кабинет — здесь же,
 * если мастер продолжили назавтра и кабинет закрыт.
 */
@Composable
internal fun AdminStep(parts: SetupParts) {
    val state = parts.state
    val actions = parts.actions
    val office = parts.office
    if (state.way == SetupWay.ViaCabinet && office != null && !office.session.open) SignInFirst(office.cabinet)
    ContourPicker(state.contours, state.contour) { actions.edit(state.form.copy(contour = it)) }
    AdminPinField(state.form.adminPin) { actions.edit(state.form.copy(adminPin = it)) }
    RepeatPinField(state.form) { actions.edit(state.form.copy(adminPinRepeat = it)) }
}
