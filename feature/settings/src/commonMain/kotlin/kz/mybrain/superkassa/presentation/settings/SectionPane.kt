package kz.mybrain.superkassa.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.onKeyEvent
import kz.mybrain.superkassa.designsystem.keyboard.escapePressedBy
import kz.mybrain.superkassa.designsystem.list.ScrollableColumn
import kz.mybrain.superkassa.designsystem.section.PaneTitle
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Открытый раздел: заголовок и группы настроек одной колонкой.
 *
 * Группы стоят одна под другой во всю ширину панели, в порядке каталога:
 * порядок не зависит от их высоты и не меняется от нажатия.
 *
 * Открытый раздел забирает ввод с клавиатуры: PageDown и стрелки листают
 * его сразу, а Escape — как стрелка в заголовке — ведёт назад к списку,
 * когда раздел открыт поверх него. Нажатие берётся на всплытии: Escape
 * раскрытого списка выбора или диалога достаётся им, а не разделу.
 *
 * @param onBack назад к списку; `null` — список стоит рядом.
 */
@Composable
internal fun SectionPane(board: SettingsBoard, section: SettingsSection, onBack: (() -> Unit)?) {
    val texts = textsOf(LocalLanguage.current)
    val focus = remember { FocusRequester() }
    Column(
        modifier = Modifier.fillMaxSize().onKeyEvent { event ->
            val back = onBack ?: return@onKeyEvent false
            escapePressedBy(event).also { if (it) back() }
        }
    ) {
        PaneTitle(section.title(texts.settings.sections), onBack, texts.common.settingsScreen.back)
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
