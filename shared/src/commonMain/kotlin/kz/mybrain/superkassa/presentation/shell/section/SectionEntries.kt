package kz.mybrain.superkassa.presentation.shell.section

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kz.mybrain.superkassa.presentation.shell.AppContainer
import kz.mybrain.superkassa.presentation.shell.frame.WindowParts

/**
 * Экраны разделов по их ключам — для `NavDisplay` окна.
 *
 * Разделы собирает каркас: им нужны службы окна, порты областей
 * и соседи, которых сами области не видят, — печать, кабинет, карта.
 */
internal fun EntryProviderScope<NavKey>.sectionEntries(app: AppContainer, window: WindowParts) {
    Section.entries.forEach { section ->
        addEntryProvider(section.key, section.key.toString(), { emptyMap() }) { SectionContent(app, window, section) }
    }
}
