package kz.mybrain.superkassa.integrations.maps

/**
 * Где хранятся полученные плитки.
 *
 * Модуль требует хранилище, а не держит его сам: где лежат файлы, знает
 * платформа — каталог данных на настольной машине, кэш приложения на
 * Android. Раз положенная плитка больше у службы не спрашивается: правила
 * OSM запрещают массовую выкачку, а карта нужна на минуту. Плитки разных
 * поставщиков лежат раздельно: карта Яндекса не должна показать плитку OSM.
 */
interface TileStore {

    /** Плитка поставщика [provider] ([TileProvider.id]) из хранилища; `null` — её там нет. */
    suspend fun read(provider: String, tile: MapTile): ByteArray?

    /** Кладёт полученную плитку; неудача записи не должна ронять показ карты. */
    suspend fun write(provider: String, tile: MapTile, image: ByteArray)

    /** Хранилища нет: каждая плитка спрашивается у службы. */
    companion object None : TileStore {
        override suspend fun read(provider: String, tile: MapTile): ByteArray? = null

        override suspend fun write(provider: String, tile: MapTile, image: ByteArray) = Unit
    }
}

/**
 * Журнал обращений к службам карты.
 *
 * В него уходит служба и имя помехи — без адреса и координат: адреса
 * торговых точек владельца в файл для поддержки не попадают.
 */
fun interface MapJournal {

    /** Записывает строку о неудаче обращения к службе [service]. */
    fun failed(service: String, reason: String)

    /** Журнала нет. */
    companion object {
        /** Ничего не записывает. */
        val Silent: MapJournal = MapJournal { _, _ -> }
    }
}
