package kz.mybrain.superkassa.domain.map.model

/**
 * Что владелец свернул на карте касс.
 *
 * Свёрнутые карточка, легенда и список касс — его выбор, и повторять нажатие при каждом
 * открытии раздела он не должен.
 */
data class MapPanels(
    val cardCollapsed: Boolean = false,
    val legendCollapsed: Boolean = false,
    val listCollapsed: Boolean = false
)

/** Сворачиваемые части карты касс. */
enum class MapPanel { Card, Legend, List }
