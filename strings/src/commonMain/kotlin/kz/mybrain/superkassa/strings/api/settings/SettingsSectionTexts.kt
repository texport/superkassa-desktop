package kz.mybrain.superkassa.strings.api.settings

/**
 * Надписи списка разделов настроек: полки, названия разделов и сводки
 * под ними.
 *
 * Список стоит слева от настроек раздела, а на телефоне открывается
 * первым: по названию и строке под ним владелец решает, куда зайти,
 * не открывая раздела. Сводка поэтому называет, что внутри, словами
 * кассира, а не перечисляет поля.
 */
data class SettingsSectionTexts(
    /** Полка разделов самой кассы: их принимает касса. */
    val shelfKkm: String,
    /** Полка разделов этой машины: они одни на все кассы рабочего места. */
    val shelfWorkplace: String,
    /** Полка самой программы: выпуски и журнал. */
    val shelfProgram: String,
    val kkm: String,
    val kkmAbout: String,
    val printing: String,
    val printingAbout: String,
    val taxes: String,
    val taxesAbout: String,
    val bfd: String,
    val bfdAbout: String,
    val look: String,
    val lookAbout: String,
    val salePanels: String,
    val salePanelsAbout: String,
    val machine: String,
    val machineAbout: String,
    val delivery: String,
    val deliveryAbout: String,
    val addresses: String,
    val addressesAbout: String,
    val updates: String,
    val updatesAbout: String,
    val debug: String,
    val debugAbout: String,
    /** Язык надписей приложения: группа в разделе оформления. */
    val appLanguage: String
)
