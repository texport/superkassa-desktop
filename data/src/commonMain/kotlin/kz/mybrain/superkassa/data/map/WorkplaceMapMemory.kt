package kz.mybrain.superkassa.data.map

import kz.mybrain.superkassa.data.local.workplace.Preferences
import kz.mybrain.superkassa.domain.map.port.MapMemory

/** Память карт — в настройках рабочего места, рядом с прочими выборами владельца. */
class WorkplaceMapMemory(private val preferences: Preferences) : MapMemory {

    override var cardCollapsed: Boolean
        get() = preferences.mapCardCollapsed
        set(value) {
            preferences.mapCardCollapsed = value
        }

    override var legendCollapsed: Boolean
        get() = preferences.mapLegendCollapsed
        set(value) {
            preferences.mapLegendCollapsed = value
        }

    override var locationAllowed: Boolean?
        get() = preferences.locationAllowed
        set(value) {
            preferences.locationAllowed = value
        }
}
