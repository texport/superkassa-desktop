package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.ui.analytics.AnalyticsKkmDialog
import kz.mybrain.superkassa.desktop.ui.analytics.sieved
import kz.mybrain.superkassa.desktop.ui.analytics.MapSieve
import kotlin.test.Test

/**
 * Снимки раскрытого ярлычка и окна аналитики одной кассы.
 *
 * Путь владельца от карты до кассы: ярлычок с числом — список касс
 * места — карточка кассы с возвратом к соседям — окно её сводки.
 * На снимке проверяется, что каждый шаг говорит, где владелец находится
 * и как вернуться назад.
 */
class AnalyticsKkmShots {

    /** Касса выбрана в раскрытом месте: карточка с возвратом к соседям. */
    @Test
    fun `касса выбрана в раскрытом месте`() {
        val view = Look.view((1..3).map { Look.kkm(it, address = HERE) })
        val model = Look.model()
        val laid = laidOut(view, mapOf(HERE to (Look.LATITUDE to Look.LONGITUDE)))
        model.centre(laid.placed)
        val groups = groupsOf(laid, model.map.zoom)
        model.spot = groups.first().id
        model.chosen = "c2"
        RenderProbe(WIDE, HIGH) { MapLook(model, sieved(laid, MapSieve()), groups, view) }
            .use { probe ->
                repeat(SETTLE) { probe.frame() }
                Look.shot("an-spot-kkm-card", probe.frame())
            }
    }

    /**
     * Окно сводки одной кассы.
     *
     * Кабинет не спрашивается: сеанс без доступа, и окно показывает
     * ожидание — ровно то, что владелец увидит до первого ответа.
     */
    @Test
    fun `окно сводки одной кассы`() {
        val session = Look.session()
        RenderProbe(WIDE, HIGH) {
            AnalyticsKkmDialog(session, CabinetSession(), Look.kkm(1), Look.texts, Look.cabinet) {}
        }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            Look.shot("an-kkm-dialog", probe.frame())
        }
    }

    /** Десяток касс в одном доме: список места не должен ни распирать экран, ни обрезаться. */
    @Test
    fun `десяток касс в одном доме`() {
        val view = Look.view((1..TEN).map { Look.kkm(it, address = HERE) })
        val model = Look.model()
        val laid = laidOut(view, mapOf(HERE to (Look.LATITUDE to Look.LONGITUDE)))
        model.centre(laid.placed)
        val groups = groupsOf(laid, model.map.zoom)
        model.spot = groups.first().id
        RenderProbe(WIDE, HIGH) { MapLook(model, laid, groups, view) }
            .use { probe ->
                repeat(SETTLE) { probe.frame() }
                Look.shot("an-spot-ten", probe.frame())
            }
    }

    private companion object {
        const val HERE = "г. Алматы, пр. Абая, 10"
        const val TEN = 10
        const val SETTLE = 24
        const val WIDE = 1180
        const val HIGH = 820
    }
}
