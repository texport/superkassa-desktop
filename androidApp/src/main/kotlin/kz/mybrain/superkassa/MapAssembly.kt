package kz.mybrain.superkassa

import android.content.Context
import kz.mybrain.superkassa.data.local.workplace.Preferences
import kz.mybrain.superkassa.data.log.AppJournal
import kz.mybrain.superkassa.data.log.LogSource
import kz.mybrain.superkassa.data.map.DiskTiles
import kz.mybrain.superkassa.data.map.MapAddresses
import kz.mybrain.superkassa.data.map.OpenStreetMaps
import kz.mybrain.superkassa.data.map.WorkplaceMapMemory
import kz.mybrain.superkassa.data.map.mapJournal
import kz.mybrain.superkassa.integrations.maps.OpenMaps
import kz.mybrain.superkassa.presentation.common.mapview.MapPorts
import kz.mybrain.superkassa.strings.api.Language
import java.io.File

/**
 * Карты на Android — те же службы, что на компьютере.
 *
 * Плитки хранятся в кэше приложения: система вправе его чистить, и это
 * правильно — плитка спросится снова, а данные кассы не пострадают.
 * Своё место от самой машины не определяется: для этого нужно разрешение
 * на геопозицию, а ради кнопки «Где я» его у кассира не спрашивают;
 * город по адресу подключения — с согласия владельца, как на компьютере.
 *
 * @param language язык кассира: на нём подписи плиток и найденные адреса.
 */
internal fun androidMaps(context: Context, preferences: Preferences, language: () -> Language): MapPorts {
    val maps = OpenMaps(
        services = MapAddresses(preferences.maps)::services,
        tiles = DiskTiles(File(context.cacheDir, TILES).path),
        journal = mapJournal(AppJournal(LogSource.App))
    )
    return MapPorts(OpenStreetMaps(maps, { language().code }), WorkplaceMapMemory(preferences))
}

/** Каталог плиток в кэше приложения. */
private const val TILES = "tiles"
