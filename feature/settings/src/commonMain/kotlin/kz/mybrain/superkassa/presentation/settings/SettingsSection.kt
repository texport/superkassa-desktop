package kz.mybrain.superkassa.presentation.settings

import androidx.compose.ui.graphics.vector.ImageVector
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.designsystem.theme.icon.Glyphs
import kz.mybrain.superkassa.presentation.settings.workplace.title
import kz.mybrain.superkassa.strings.api.Texts
import kz.mybrain.superkassa.strings.api.settings.SettingsSectionTexts

/**
 * Полки списка разделов: чьё это.
 *
 * Касса принимает свои настройки сама и только для себя; рабочее место —
 * это машина со всеми её кассами; программа — выпуски и журнал. Полка
 * отвечает на вопрос, где изменится выбранное, ещё до того, как раздел
 * открыт.
 */
internal enum class SettingsShelf(val title: (SettingsSectionTexts) -> String) {
    Kkm({ it.shelfKkm }),
    Workplace({ it.shelfWorkplace }),
    Program({ it.shelfProgram })
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
 * Порядок — порядок в списке: сперва сама касса, от повседневного
 * к редкому, потом машина, потом программа.
 */
internal enum class SettingsSection(
    val shelf: SettingsShelf,
    val icon: ImageVector,
    val title: (SettingsSectionTexts) -> String,
    val about: (SettingsSectionTexts) -> String
) {
    Kkm(SettingsShelf.Kkm, AppIcons.sectionKkm, { it.kkm }, { it.kkmAbout }),
    Printing(SettingsShelf.Kkm, AppIcons.sectionPrinting, { it.printing }, { it.printingAbout }),
    Taxes(SettingsShelf.Kkm, AppIcons.sectionTaxes, { it.taxes }, { it.taxesAbout }),
    Bfd(SettingsShelf.Kkm, AppIcons.sectionBfd, { it.bfd }, { it.bfdAbout }),
    Look(SettingsShelf.Workplace, AppIcons.sectionLook, { it.look }, { it.lookAbout }),
    SalePanels(SettingsShelf.Workplace, AppIcons.sectionSalePanels, { it.salePanels }, { it.salePanelsAbout }),
    Machine(SettingsShelf.Workplace, AppIcons.sectionMachine, { it.machine }, { it.machineAbout }),
    Delivery(SettingsShelf.Workplace, AppIcons.receiptDelivery, { it.delivery }, { it.deliveryAbout }),
    Addresses(SettingsShelf.Workplace, AppIcons.sectionAddresses, { it.addresses }, { it.addressesAbout }),
    Updates(SettingsShelf.Program, AppIcons.update, { it.updates }, { it.updatesAbout }),
    Debug(SettingsShelf.Program, AppIcons.debug, { it.debug }, { it.debugAbout })
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
 * У кассы — её название и режим программирования: по списку видно,
 * настройки какой кассы открыты и принимает ли она их сейчас. У вида —
 * выбранная тема и язык. Остальные разделы называют, что в них лежит.
 */
internal fun SettingsSection.summary(board: SettingsBoard, texts: Texts): String {
    val sections = texts.settings.sections
    return when (this) {
        SettingsSection.Kkm -> listOfNotNull(
            board.kkm.displayName.ifBlank { null },
            texts.kassa.money.kkm.programmingOn.takeIf { board.kkm.programming }
        ).joinToString(Glyphs.SEPARATOR).ifBlank { about(sections) }
        SettingsSection.Look -> listOf(
            board.look.appearance.title(texts.common.settingsScreen),
            board.look.language.title
        ).joinToString(Glyphs.SEPARATOR)
        else -> about(sections)
    }
}
