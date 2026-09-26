package kz.mybrain.superkassa.data.local.workplace

import kz.mybrain.superkassa.data.map.mapProviders
import kz.mybrain.superkassa.data.map.publicMaps
import kz.mybrain.superkassa.domain.map.model.MapProvider
import kz.mybrain.superkassa.domain.workplace.model.MapServices
import kz.mybrain.superkassa.domain.workplace.port.WorkplaceChoices

/**
 * Настройки этой машины — в тех же файлах, что и прочая память рабочего места.
 *
 * Своего хранилища здесь нет: адрес кабинета, службы карты, отрасль
 * и своё название кассы лежат там, откуда их читают кабинет, карта
 * и продажа.
 */
class PreferenceChoices(private val preferences: Preferences) : WorkplaceChoices {

    override var cabinetUrl: String
        get() = preferences.cabinetUrl
        set(value) {
            preferences.cabinetUrl = value
        }

    override var cabinetServer: String
        get() = preferences.cabinetServer
        set(value) {
            preferences.cabinetServer = value
        }

    override var maps: MapServices
        get() = preferences.maps.run { MapServices(tiles, search, reverse, location, provider) }
        set(value) {
            preferences.maps.tiles = value.tiles
            preferences.maps.search = value.search
            preferences.maps.reverse = value.reverse
            preferences.maps.location = value.location
            preferences.maps.provider = value.provider
        }

    override val publicMaps: MapServices = publicMaps()

    override val mapProviders: List<MapProvider> = mapProviders()

    override fun chooseDomain(kkmId: String, code: String?) = preferences.chooseDomain(kkmId, code)

    override fun rename(kkmId: String, name: String?) = preferences.rename(kkmId, name)
}
