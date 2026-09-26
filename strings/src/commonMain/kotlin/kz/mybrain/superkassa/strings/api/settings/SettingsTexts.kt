package kz.mybrain.superkassa.strings.api.settings

/**
 * Надписи области «Настройки», которых нет в общем наборе настроек.
 *
 * Общий набор `CommonTexts.settingsScreen` держит надписи самого экрана; здесь —
 * карточки, у которых своя тема: настройки кассы в процессе, доставка чека
 * покупателю и сведения о кассе.
 */
data class SettingsTexts(
    /** Настройки кассы на этой машине: сроки ожидания и связь с БФД. */
    val core: CoreSettingTexts,
    /** Каналы доставки чека покупателю. */
    /** Сведения о кассе. */
    val facts: KassaFactsTexts,
    /** Список разделов настроек: полки, названия и сводки. */
    val sections: SettingsSectionTexts
)
