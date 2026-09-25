package kz.mybrain.superkassa.presentation.analytics.map

import kz.mybrain.superkassa.OwnerShots
import kz.mybrain.superkassa.presentation.analytics.AnalyticsLook
import kotlin.test.Test

/** Карта касс по замечаниям владельца — в окне ноутбука и на мониторе, `/tmp/owner-map-<окно>.png`. */
class AnalyticsOwnerMapShots {

    @Test
    fun `карта касс`() = OwnerShots.each { width, height ->
        val view = AnalyticsLook.view((1..MAP_KKMS).map { AnalyticsLook.kkm(it, address = HERE) })
        val start = AnalyticsLook.model()
        val laid = laidOut(view, mapOf(HERE to (AnalyticsLook.LATITUDE to AnalyticsLook.LONGITUDE)))
        AnalyticsLook.centre(start, laid.placed)
        OwnerShots.save("map", width, height) { MapLook(start, laid, groupsOf(laid, start.map.zoom), view) }
    }

    private companion object {
        const val MAP_KKMS = 5
        const val HERE = "Алматы, пр. Абая, 1"
    }
}
