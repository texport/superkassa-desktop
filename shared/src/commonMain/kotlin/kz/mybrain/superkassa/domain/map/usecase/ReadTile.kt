package kz.mybrain.superkassa.domain.map.usecase

import kz.mybrain.superkassa.domain.map.port.Maps

/** Плитка карты: с диска рабочего места, а нет там — у службы. */
class ReadTile(private val maps: Maps) {

    /** @return картинка плитки; `null` — взять её неоткуда, карта остаётся сеткой. */
    suspend operator fun invoke(zoom: Int, x: Int, y: Int): ByteArray? = maps.tile(zoom, x, y)
}
