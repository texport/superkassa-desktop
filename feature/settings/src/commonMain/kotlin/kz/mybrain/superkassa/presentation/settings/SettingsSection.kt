package kz.mybrain.superkassa.presentation.settings

import androidx.compose.ui.graphics.vector.ImageVector
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.designsystem.theme.icon.Glyphs
import kz.mybrain.superkassa.presentation.settings.workplace.summary
import kz.mybrain.superkassa.strings.api.Texts
import kz.mybrain.superkassa.strings.api.settings.SettingsSectionTexts

/**
 * Полки списка разделов: чьё это.
 *
 * Настройки бывают трёх хозяев, и полка называет хозяина раньше, чем
 * открыт раздел: выбранная касса принимает свои настройки сама и только
 * для себя; приложение — вид, язык, журнал и версии на этой машине;
 * кабинет БФД — куда рабочее место к нему подключается. Касса выбрана
 * только после входа: до входа её полки нет.
 */
internal enum class SettingsShelf(val title: (SettingsSectionTexts) -> String) {
    Kkm({ it.shelfKkm }),
    App({ it.shelfApp }),
    Cabinet({ it.shelfCabinet })
}

/**
 * Раздел настроек: строка списка слева и настройки справа.
 *
 * Прежде настройки стояли вкладками «Приложение / Касса / Кабинет БФД»,
 * а под вкладкой карточки раскладывались столбцами по ширине окна:
 * на большом мониторе они вставали вразнобой по высоте, и владелец искал
 * нужную глазами по всему экрану. Теперь, как велит Material 3 для
 * настроек на большом экране, слева список разделов, справа — один
 * открытый раздел одной колонкой.
 *
 * Порядок — порядок в списке: сперва выбранная касса, от повседневного
 * к редкому, потом приложение, потом кабинет.
 */
internal enum class SettingsSection(
    val shelf: SettingsShelf,
    val icon: ImageVector,
    val title: (SettingsSectionTexts) -> String,
    val about: (SettingsSectionTexts) -> String
) {
    General(SettingsShelf.Kkm, AppIcons.sectionKkm, { it.general }, { it.generalAbout }),
    Printing(SettingsShelf.Kkm, AppIcons.sectionPrinting, { it.printing }, { it.printingAbout }),
    Taxes(SettingsShelf.Kkm, AppIcons.sectionTaxes, { it.taxes }, { it.taxesAbout }),
    Bfd(SettingsShelf.Kkm, AppIcons.sectionBfd, { it.bfd }, { it.bfdAbout }),
    Look(SettingsShelf.App, AppIcons.sectionLook, { it.look }, { it.lookAbout }),
    Language(SettingsShelf.App, AppIcons.sectionLanguage, { it.language }, { it.languageAbout }),
    SalePanels(SettingsShelf.App, AppIcons.sectionSalePanels, { it.salePanels }, { it.salePanelsAbout }),
    Debug(SettingsShelf.App, AppIcons.debug, { it.debug }, { it.debugAbout }),
    About(SettingsShelf.App, AppIcons.info, { it.about }, { it.aboutAbout }),
    Connection(SettingsShelf.Cabinet, AppIcons.sectionAddresses, { it.connection }, { it.connectionAbout })
}

/** Разделы, в которых при таком месте, правах, кабинете и выпусках есть что показать. */
internal fun visibleSections(
    hasRegister: Boolean,
    admin: Boolean,
    hasCabinet: Boolean = true,
    hasReleases: Boolean = true
): List<SettingsSection> = visibleSettings(hasRegister, admin, hasCabinet, hasReleases).map { it.section }.distinct()

/** Разделы, которые видит тот, для кого собрана доска. */
internal val SettingsBoard.sections: List<SettingsSection>
    get() = visibleSections(kkm.kkm != null, kkm.admin, parts.hasCabinet, parts.hasReleases)

/** Настройки раздела на этой доске — в порядке показа. */
internal fun SettingsBoard.cardsOf(section: SettingsSection): List<SettingsCard> = settingsCards.filter {
    it.setting.section == section && it.visible(kkm.kkm != null, kkm.admin, parts.hasCabinet, parts.hasReleases)
}

/**
 * Строка под названием раздела.
 *
 * У основного раздела кассы — её название и режим программирования:
 * по списку видно, настройки какой кассы открыты и принимает ли она их
 * сейчас. У оформления — выбранные тема и тон, у языка — сам язык,
 * у сведений о программе — её версия. Остальные разделы называют, что
 * в них лежит.
 */
internal fun SettingsSection.summary(board: SettingsBoard, texts: Texts): String {
    val sections = texts.settings.sections
    return when (this) {
        SettingsSection.General -> listOfNotNull(
            board.kkm.displayName.ifBlank { null },
            texts.kassa.money.kkm.programmingOn.takeIf { board.kkm.programming }
        ).joinToString(Glyphs.SEPARATOR).ifBlank { about(sections) }
        SettingsSection.Look -> board.look.summary(texts.common.settingsScreen)
        SettingsSection.Language -> board.look.language.title
        SettingsSection.About ->
            board.core.about?.let { "${texts.settings.facts.appVersion} ${it.appVersion}" } ?: about(sections)
        else -> about(sections)
    }
}
