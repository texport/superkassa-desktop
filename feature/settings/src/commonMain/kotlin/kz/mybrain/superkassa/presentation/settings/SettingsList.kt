package kz.mybrain.superkassa.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import kz.mybrain.superkassa.designsystem.list.ScrollableColumn
import kz.mybrain.superkassa.designsystem.list.SectionListItem
import kz.mybrain.superkassa.designsystem.section.PaneTitle
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Список разделов настроек по полкам: касса, рабочее место, программа.
 *
 * Строка — значок, название раздела и сводка под ним: по ней видно, что
 * внутри, не открывая. Полка без видимых разделов не показывается вовсе —
 * кассиру и до входа нечего делать с полкой кассы.
 *
 * @param open раздел, открытый в панели рядом; `null` — рядом ничего нет,
 *   и подсвечивать нечего.
 * @param onOpen раздел выбран: он открывается рядом или поверх списка.
 */
@Composable
internal fun SettingsList(
    board: SettingsBoard,
    sections: List<SettingsSection>,
    open: SettingsSection?,
    titled: Boolean,
    onOpen: (SettingsSection) -> Unit
) {
    val texts = textsOf(LocalLanguage.current)
    Column(modifier = Modifier.fillMaxSize()) {
        if (titled) PaneTitle(texts.common.settingsScreen.title)
        ScrollableColumn(modifier = Modifier.weight(1f), spacing = Spacing.inline) {
            SettingsShelf.entries.forEach { shelf ->
                val shelved = sections.filter { it.shelf == shelf }
                if (shelved.isEmpty()) return@forEach
                ShelfTitle(shelf.title(texts.settings.sections))
                shelved.forEach { section ->
                    SectionListItem(
                        icon = section.icon,
                        title = section.title(texts.settings.sections),
                        summary = section.summary(board, texts),
                        selected = section == open,
                        onClick = { onOpen(section) }
                    )
                }
            }
        }
    }
}

/** Подзаголовок полки — подзаголовок списка Material 3: `primary`, с вертикали строк. */
@Composable
private fun ShelfTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(horizontal = Spacing.cardPadding, vertical = Spacing.itemGap)
            .semantics { heading() }
    )
}
