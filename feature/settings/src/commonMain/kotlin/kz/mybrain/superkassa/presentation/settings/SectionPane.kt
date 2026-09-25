package kz.mybrain.superkassa.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import kz.mybrain.superkassa.designsystem.list.ScrollableColumn
import kz.mybrain.superkassa.designsystem.section.PaneTitle
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.presentation.common.navigation.ScreenBar
import kz.mybrain.superkassa.strings.api.settings.SettingsSectionTexts
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Открытый раздел: группы настроек одной колонкой.
 *
 * Группы стоят одна под другой во всю ширину панели, в порядке каталога:
 * порядок не зависит от их высоты и не меняется от нажатия.
 *
 * Рядом со списком у панели свой заголовок — раздел; шапка окна называет
 * настройки целиком. Поверх списка — на узком окне — заголовка у панели
 * нет: раздел называет шапка окна, под ним — чьи это настройки, и она же
 * держит стрелку назад по истории окна.
 *
 * Открытый раздел забирает ввод с клавиатуры: PageDown и стрелки листают
 * его сразу.
 *
 * @param beside раздел стоит рядом со списком.
 */
@Composable
internal fun SectionPane(board: SettingsBoard, section: SettingsSection, beside: Boolean) {
    val texts = textsOf(LocalLanguage.current)
    val title = section.title(texts.settings.sections)
    val focus = remember { FocusRequester() }
    if (!beside) ScreenBar(title, section.owner(board, texts.settings.sections))
    Column(modifier = Modifier.fillMaxSize()) {
        if (beside) PaneTitle(title)
        ScrollableColumn(
            modifier = Modifier.weight(1f),
            spacing = Spacing.sectionGap,
            gutter = Spacing.flush,
            focus = focus
        ) {
            board.cardsOf(section).forEach { it.card(board) }
        }
    }
    LaunchedEffect(section) { focus.requestFocus() }
}

/**
 * Чьи это настройки — коротко, как группа в списке: касса по имени,
 * «Приложение» или «Кабинет БФД». Длинное «Настройки кассы «…»» на
 * телефоне обрезалось раньше имени кассы — ради которого и стоит.
 */
private fun SettingsSection.owner(board: SettingsBoard, texts: SettingsSectionTexts): String =
    board.kkm.displayName.takeIf { shelf == SettingsShelf.Kkm && it.isNotBlank() } ?: shelf.title(texts)
