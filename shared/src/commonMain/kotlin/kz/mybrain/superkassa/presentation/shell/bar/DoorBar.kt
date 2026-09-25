package kz.mybrain.superkassa.presentation.shell.bar

import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.designsystem.section.AppTopBar
import kz.mybrain.superkassa.designsystem.section.BarLead
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.presentation.cabinet.CabinetBar
import kz.mybrain.superkassa.presentation.common.picker.LanguagePicker
import kz.mybrain.superkassa.presentation.common.picker.ThemeSwitch
import kz.mybrain.superkassa.presentation.shell.frame.WindowParts
import kz.mybrain.superkassa.presentation.users.signin.Door
import kz.mybrain.superkassa.strings.api.common.CommonTexts

/**
 * Шапка окна до входа — одна на вход и на всё, что открыто его дверями.
 *
 * Прежде у каждой двери была своя шапка: у настроек — серая полоса внутри
 * полей окна, у кабинета — шапка кабинета, у заведения кассы — никакой,
 * со стрелкой внутри мастера, а у самого входа — крупный заголовок
 * в колонке. Шапка прыгала от двери к двери. Теперь она стоит в слоте
 * `Scaffold` каркаса, как у рабочего окна (Material 3: один `TopAppBar`
 * на окно): на входе — «Вход в кассу» с темой и языком, за дверью —
 * название двери и стрелка назад на вход; у кабинета — его шапка, та же,
 * что у раздела кабинета после входа.
 *
 * @param close закрыть дверь и вернуться на вход.
 */
@Composable
internal fun DoorBar(window: WindowParts, door: Door, close: () -> Unit) {
    val texts = LocalStrings.current
    val cabinet = window.cabinet
    if (door == Door.Cabinet && cabinet != null) {
        CabinetBar(cabinet.cabinet, cabinet.look, onExit = close)
        return
    }
    val atDoor = door == Door.Kkms
    AppTopBar(
        title = door.title(texts),
        lead = BarLead.Back(close, texts.settingsScreen.back).takeUnless { atDoor }
    ) {
        if (atDoor) {
            ThemeSwitch(window.look)
            LanguagePicker(window.look)
        }
    }
}

/** Как называется дверь в шапке: так же, как раздел рабочего окна за ней. */
private fun Door.title(texts: CommonTexts): String = when (this) {
    Door.Register -> texts.sections.register
    Door.Cabinet -> texts.sections.cabinet
    Door.Settings -> texts.sections.settings
    Door.Kkms -> texts.login.title
}
