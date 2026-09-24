package kz.mybrain.superkassa.data.map

import kz.mybrain.superkassa.domain.workplace.model.MapServices
import kz.mybrain.superkassa.integrations.maps.MapServices as ModuleServices

/**
 * Общедоступные службы модуля карт — словами настроек рабочего места.
 *
 * Адреса по умолчанию знает модуль карт, и только он: настройки показывают
 * их подсказкой в пустых полях, а не держат свою копию.
 */
internal fun publicMaps(): MapServices = ModuleServices().run { MapServices(tiles, search, reverse, location) }
