package kz.mybrain.superkassa.integrations.maps

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class TileProvidersTest {
    private val almaty = MapTile(12, 2923, 1505)

    @Test
    fun openStreetMapIsTheDefault() {
        assertSame(TileProviders.OpenStreetMap, TileProviders.all.first())
        assertSame(TileProviders.OpenStreetMap, TileProviders.byId(null))
        assertSame(TileProviders.OpenStreetMap, TileProviders.byId("unknown"))
        assertEquals("https://tile.openstreetmap.org/12/2923/1505.png", TileProviders.OpenStreetMap.url(almaty, "ru"))
    }

    @Test
    fun subdomainAndLanguageFillTheTemplate() {
        assertEquals("https://tile0.maps.2gis.com/tiles?x=2923&y=1505&z=12&v=1", TileProviders.TwoGis.url(almaty, "ru"))
        assertEquals(
            "https://core-renderer-tiles.maps.yandex.net/tiles?l=map&x=2923&y=1505&z=12&scale=1&lang=kk_KZ",
            TileProviders.Yandex.url(almaty, "kk")
        )
        assertEquals(
            "https://mt0.google.com/vt/lyrs=m&x=2923&y=1505&z=12&hl=en",
            TileProviders.Google.url(almaty, "en")
        )
    }

    @Test
    fun ownTileServerIsOsmFormat() {
        val own = TileProviders.custom("https://tiles.bfd.kz/")
        assertEquals("https://tiles.bfd.kz/12/2923/1505.png", own.url(almaty, "ru"))
        assertEquals(TileProviders.CUSTOM, own.id)
    }

    @Test
    fun providerIdsAreDistinct() {
        assertEquals(TileProviders.all.size, TileProviders.all.map { it.id }.toSet().size)
    }
}
