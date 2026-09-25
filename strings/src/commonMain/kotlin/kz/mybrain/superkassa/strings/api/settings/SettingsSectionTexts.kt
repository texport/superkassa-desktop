package kz.mybrain.superkassa.strings.api.settings

/**
 * Надписи списка разделов настроек: полки, названия разделов и сводки
 * под ними.
 *
 * Полки отвечают на вопрос, чьё это: выбранной кассы, самого приложения
 * на этой машине или кабинета БФД. По названию и строке под ним владелец
 * решает, куда зайти, не открывая раздела.
 */
data class SettingsSectionTexts(
    /** Полка выбранной кассы: её настройки принимает сама касса. */
    val shelfKkm: String,
    /** Полка приложения на этой машине: вид, язык, журнал, версии. */
    val shelfApp: String,
    /** Полка кабинета БФД: как рабочее место к нему подключается. */
    val shelfCabinet: String,
    val general: String,
    val generalAbout: String,
    val printing: String,
    val printingAbout: String,
    val taxes: String,
    val taxesAbout: String,
    val bfd: String,
    val bfdAbout: String,
    val delivery: String,
    val deliveryAbout: String,
    val look: String,
    val lookAbout: String,
    val language: String,
    val languageAbout: String,
    val salePanels: String,
    val salePanelsAbout: String,
    val debug: String,
    val debugAbout: String,
    val about: String,
    val aboutAbout: String,
    val connection: String,
    val connectionAbout: String,
    /** Язык надписей приложения: группа в разделе языка. */
    val appLanguage: String
)
