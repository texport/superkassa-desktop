package kz.mybrain.superkassa.data.map

import kz.mybrain.superkassa.domain.map.model.MapProvider
import kz.mybrain.superkassa.domain.map.model.TileGrid
import kz.mybrain.superkassa.domain.workplace.model.MapServices
import kz.mybrain.superkassa.integrations.maps.TileProvider
import kz.mybrain.superkassa.integrations.maps.TileProviders
import kz.mybrain.superkassa.integrations.maps.MapServices as ModuleServices
import kz.mybrain.superkassa.integrations.maps.TileGrid as ModuleGrid

/**
 * Общедоступные службы модуля карт — словами настроек рабочего места.
 *
 * Адреса по умолчанию и каталог поставщиков плиток знает модуль карт,
 * и только он: настройки показывают их подсказкой в пустых полях и списком
 * выбора, а не держат свою копию.
 */
internal fun publicMaps(): MapServices =
    ModuleServices().run { MapServices(ModuleServices.TILES, search, reverse, location, provider.id) }

/** Поставщики плиток каталога модуля карт — словами предметной области. */
internal fun mapProviders(): List<MapProvider> = TileProviders.all.map { it.domain() }

/** Поставщик модуля карт — словами предметной области. */
internal fun TileProvider.domain(): MapProvider = MapProvider(
    id = id,
    name = name,
    attribution = attribution,
    grid = if (grid == ModuleGrid.EllipticalMercator) TileGrid.EllipticalMercator else TileGrid.WebMercator,
    maxZoom = maxZoom
)
