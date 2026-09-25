package kz.mybrain.superkassa.designsystem.adaptive

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import kz.mybrain.superkassa.designsystem.theme.size.Spacing

/**
 * Где окну ставить снекбар: по центру рабочей части, а не над
 * вспомогательной панелью справа.
 *
 * Снекбар по Material 3 стоит внизу по центру. Но на экране продажи
 * справа касса с «Пробить чек», и по центру окна снекбар закрывал кнопку,
 * пока кассир читал, почему её нажатие не прошло; прижатый же к левому
 * краю, он стоял не по центру. Панель, стоящая сбоку ([SupportingPanes]),
 * сообщает здесь, сколько места справа она занимает, и снекбар встаёт
 * по центру оставшегося.
 *
 * Держит один на окно каркас; без него снекбар встаёт по центру окна.
 */
class SnackbarRoom {
    /** Сколько места справа занято вспомогательной панелью. */
    var end: Dp by mutableStateOf(Spacing.flush)
}

/** Место снекбара окна; каркас ставит его один раз на всё окно. */
val LocalSnackbarRoom = staticCompositionLocalOf { SnackbarRoom() }
