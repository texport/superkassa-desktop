package kz.mybrain.superkassa.domain.map.usecase

import kz.mybrain.superkassa.domain.map.model.MapProvider
import kz.mybrain.superkassa.domain.map.port.Maps

/** Чьи плитки у карты сейчас: по ним карта ставит плитки и подписывает авторство. */
class ReadMapProvider(private val maps: Maps) {
    operator fun invoke(): MapProvider = maps.provider()
}
