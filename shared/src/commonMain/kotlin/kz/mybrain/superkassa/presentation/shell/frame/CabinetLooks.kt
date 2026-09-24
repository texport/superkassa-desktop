package kz.mybrain.superkassa.presentation.shell.frame

import kz.mybrain.superkassa.presentation.cabinet.CabinetLook
import kz.mybrain.superkassa.presentation.common.look.LookViewModel
import kz.mybrain.superkassa.presentation.common.model.collectAsScreenState
import kz.mybrain.superkassa.presentation.common.picker.LanguagePicker
import kz.mybrain.superkassa.presentation.common.picker.ThemeSwitch

/**
 * Вид окна глазами кабинета: колонка точек и переключатели шапки.
 *
 * Собирается здесь, в каркасе: каркас видит и настройки, где живёт
 * модель вида, и кабинет, которому нужны три её действия.
 */
fun cabinetLook(look: LookViewModel): CabinetLook = CabinetLook(
    placesCollapsed = { look.state.collectAsScreenState().value.placesCollapsed },
    togglePlaces = look::togglePlaces,
    switches = {
        ThemeSwitch(look)
        LanguagePicker(look)
    }
)
