package kz.mybrain.superkassa.presentation.shell.bar

import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.designsystem.section.AppTopBar
import kz.mybrain.superkassa.designsystem.section.BarAction
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.presentation.cabinet.CabinetBar
import kz.mybrain.superkassa.presentation.common.navigation.LocalScreenBar
import kz.mybrain.superkassa.presentation.common.picker.LanguagePicker
import kz.mybrain.superkassa.presentation.common.picker.ThemeSwitch
import kz.mybrain.superkassa.presentation.shell.frame.WindowParts
import kz.mybrain.superkassa.presentation.shell.section.DoorSection
import kz.mybrain.superkassa.strings.api.common.CommonTexts

/**
 * Шапка окна до входа — та же, что у рабочего окна.
 *
 * Стоит в слоте `Scaffold` рамки окна (Material 3: один `TopAppBar`
 * на окно) и называет открытый раздел: у касс — «Вход в кассу», у прочих —
 * их название; у кабинета — шапка кабинета, та же, что после входа.
 * Разделы — верхний уровень навигации, и стрелки назад у них нет; она
 * появляется у шага внутри раздела, который сам себя называет. Тема
 * и язык — в каждом разделе: до входа их больше негде переключить.
 *
 * @param onMenu открыть разделы окна — на телефоне, где их не видно.
 * @param onBack снять шаг внутри раздела; `null` — открыт сам раздел.
 * @param onReload перечитать кассы — у раздела касс; действие экрана
 *   стоит в шапке, как «Обновить» рабочего окна, а не в полосе пина.
 */
@Composable
internal fun DoorBar(
    window: WindowParts,
    door: DoorSection,
    onMenu: (() -> Unit)?,
    onBack: (() -> Unit)?,
    onReload: (() -> Unit)? = null
) {
    val texts = LocalStrings.current
    val cabinet = window.cabinet
    if (door == DoorSection.Cabinet && cabinet != null) {
        CabinetBar(cabinet.cabinet, cabinet.look, onMenu = onMenu.takeIf { onBack == null }, onStep = onBack)
        return
    }
    val step = LocalScreenBar.current
    AppTopBar(title = step.title ?: door.barTitle(texts), subtitle = step.subtitle, lead = barLead(onMenu, onBack)) {
        onReload?.let { BarAction(AppIcons.refresh, texts.login.reload, it) }
        ThemeSwitch(window.look)
        LanguagePicker(window.look)
    }
}

/** Как раздел назван в шапке: кассы — входом, прочие — как в навигации. */
private fun DoorSection.barTitle(texts: CommonTexts): String =
    if (this == DoorSection.Kkms) texts.login.title else title(texts.sections)
