package kz.mybrain.superkassa.presentation.shell.section

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kz.mybrain.superkassa.navigation.step.PlaceCardKey
import kz.mybrain.superkassa.navigation.step.ReturnBasisKey
import kz.mybrain.superkassa.navigation.step.SettingsSectionKey
import kz.mybrain.superkassa.navigation.step.SetupStepKey
import kz.mybrain.superkassa.presentation.shell.AppContainer
import kz.mybrain.superkassa.presentation.shell.frame.WindowParts

/**
 * Экраны разделов по их ключам — для `NavDisplay` окна.
 *
 * Разделы собирает каркас: им нужны службы окна, порты областей
 * и соседи, которых сами области не видят, — печать, кабинет, карта.
 */
/**
 * @param step переход шага вглубь и обратно — метаданные записей шагов.
 */
internal fun EntryProviderScope<NavKey>.sectionEntries(app: AppContainer, window: WindowParts, step: Map<String, Any>) {
    Section.entries.forEach { section ->
        addEntryProvider(section.key, section.key.toString(), { emptyMap() }) { SectionContent(app, window, section) }
    }
    // Подробности поверх списка — на узком окне; на широком они стоят
    // рядом со списком и шагом истории не бывают.
    entry<SettingsSectionKey>(metadata = step) { SectionContent(app, window, Section.Settings, it) }
    entry<ReturnBasisKey>(metadata = step) { SectionContent(app, window, Section.Returns, it) }
    entry<PlaceCardKey>(metadata = step) { SectionContent(app, window, Section.Cabinet, it) }
    // Шаги мастера подключения — поверх его первого шага.
    entry<SetupStepKey>(metadata = step) { SectionContent(app, window, Section.Register, it) }
}
