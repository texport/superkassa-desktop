package kz.mybrain.superkassa.presentation.cabinet

import kz.mybrain.superkassa.presentation.common.look.LookViewModel
import kz.mybrain.superkassa.presentation.common.model.collectAsScreenState
import kz.mybrain.superkassa.presentation.common.picker.LanguagePicker
import kz.mybrain.superkassa.presentation.common.picker.ThemeSwitch

/**
 * Вид окна глазами кабинета: колонка точек и переключатели шапки.
 *
 * Модель вида окна — общая, её держит каркас; кабинет берёт у неё только
 * три вещи, и разметке кабинета остальная модель не видна.
 */
fun cabinetLook(look: LookViewModel): CabinetLook = CabinetLook(
    placesCollapsed = { look.state.collectAsScreenState().value.placesCollapsed },
    togglePlaces = look::togglePlaces,
    switches = {
        ThemeSwitch(look)
        LanguagePicker(look)
    }
)
