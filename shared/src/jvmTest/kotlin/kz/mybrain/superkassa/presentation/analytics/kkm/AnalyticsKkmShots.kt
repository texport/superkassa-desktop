package kz.mybrain.superkassa.presentation.analytics.kkm

import kotlinx.coroutines.awaitCancellation
import kz.mybrain.superkassa.Look
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.data.analytics.CabinetReplies
import kz.mybrain.superkassa.domain.analytics.model.AnalyticsAnswer
import kz.mybrain.superkassa.domain.analytics.model.SalesFigures
import kz.mybrain.superkassa.domain.analytics.model.SalesFilter
import kz.mybrain.superkassa.domain.analytics.port.Analytics
import kz.mybrain.superkassa.domain.analytics.port.FakeAnalytics
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.FakeCore
import kz.mybrain.superkassa.kassa.app
import kz.mybrain.superkassa.presentation.analytics.AnalyticsLook
import kz.mybrain.superkassa.presentation.analytics.analyzing
import kz.mybrain.superkassa.presentation.analytics.map.MapLook
import kz.mybrain.superkassa.presentation.analytics.map.MapSieve
import kz.mybrain.superkassa.presentation.analytics.map.MapWords
import kz.mybrain.superkassa.presentation.analytics.map.groupsOf
import kz.mybrain.superkassa.presentation.analytics.map.laidOut
import kz.mybrain.superkassa.presentation.analytics.map.sieved
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
        val view = AnalyticsLook.view((1..3).map { AnalyticsLook.kkm(it, address = HERE) })
        val start = AnalyticsLook.model()
        val laid = laidOut(view, mapOf(HERE to (AnalyticsLook.LATITUDE to AnalyticsLook.LONGITUDE)))
        AnalyticsLook.centre(start, laid.placed)
        val groups = groupsOf(laid, start.map.zoom)
        val model = start.copy(spot = groups.first().id, chosen = "c2")
        RenderProbe(WIDE, HIGH) { MapLook(model, sieved(laid, MapSieve()), groups, view) }
            .use { probe ->
                repeat(SETTLE) { probe.frame() }
                Look.shot("an-spot-kkm-card", probe.frame())
            }
    }

    /**
     * Окно сводки одной кассы до первого ответа.
     *
     * Сеанс без доступа: кабинет не спрашивается вовсе, и на месте
     * сводки стоит ожидание — ровно то, что владелец увидит, открыв окно.
     */
    @Test
    fun `окно сводки ждёт ответа`() {
        dialog("an-kkm-dialog-waiting", waiting())
    }

    /** Пустой срок: за него ничего не продано, и об этом сказано словами. */
    @Test
    fun `окно сводки за пустой срок`() = dialog("an-kkm-dialog-empty", CabinetReplies.always(NOTHING).analytics)

    /** Раздел сводки ещё не выложен: кабинет отвечает `404`. */
    @Test
    fun `окно сводки до выкладки кабинета`() =
        dialog("an-kkm-dialog-not-deployed", CabinetReplies.always(NOT_FOUND, status = 404).analytics)

    /** Кабинет отказал по существу: отказ показывается его же словами. */
    @Test
    fun `окно сводки при отказе кабинета`() =
        dialog("an-kkm-dialog-refused", CabinetReplies.always(REFUSAL, status = 403).analytics)

    /** Кабинет, который не отвечает вовсе: окно ждёт. */
    private fun waiting(): Analytics = object : Analytics by FakeAnalytics() {
        override suspend fun sales(filter: SalesFilter): AnalyticsAnswer<SalesFigures> = awaitCancellation()
    }

    private fun dialog(name: String, cabinet: Analytics) {
        val app = CoreScene.app(FakeCore()).analyzing(cabinet)
        RenderProbe(WIDE, HIGH) {
            val words = MapWords(AnalyticsLook.texts, Look.cabinet)
            AnalyticsKkmDialog(app.areas.analytics, AnalyticsLook.kkm(1), OWNER, words) {}
        }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            Look.shot(name, probe.frame())
        }
    }

    /** Десяток касс в одном доме: список места не должен ни распирать экран, ни обрезаться. */
    @Test
    fun `десяток касс в одном доме`() {
        val view = AnalyticsLook.view((1..TEN).map { AnalyticsLook.kkm(it, address = HERE) })
        val start = AnalyticsLook.model()
        val laid = laidOut(view, mapOf(HERE to (AnalyticsLook.LATITUDE to AnalyticsLook.LONGITUDE)))
        AnalyticsLook.centre(start, laid.placed)
        val groups = groupsOf(laid, start.map.zoom)
        val model = start.copy(spot = groups.first().id)
        RenderProbe(WIDE, HIGH) { MapLook(model, laid, groups, view) }
            .use { probe ->
                repeat(SETTLE) { probe.frame() }
                Look.shot("an-spot-ten", probe.frame())
            }
    }

    private companion object {
        /** Отметка вошедшего владельца. */
        const val OWNER = "owner-1"
        const val HERE = "г. Алматы, пр. Абая, 10"
        const val TEN = 10

        /** Кабинет посчитал срок и вернул нули: документов за него нет. */
        const val NOTHING = "{}"

        const val NOT_FOUND = """{"code":"NOT_FOUND","message":"No handler"}"""
        const val REFUSAL = """{"code":"FORBIDDEN","message":"Сводка выдана не этой компании"}"""

        const val SETTLE = 24
        const val WIDE = 1180
        const val HIGH = 820
    }
}
