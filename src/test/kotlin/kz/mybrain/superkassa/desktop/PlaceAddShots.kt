package kz.mybrain.superkassa.desktop

import io.ktor.http.HttpStatusCode
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.ui.cabinet.AddPlaceCard
import kotlin.test.Test

/**
 * Снимки окна заведения торговой точки.
 *
 * Отказные случаи здесь про адресный регистр: он ответил пусто и он
 * не ответил вовсе. На экране они пока неотличимы — обёртка сеанса
 * сводит неудачу к `null`, и шаг подбора видит тот же пустой список, —
 * но молчать не должен ни один из них.
 */
class PlaceAddShots {

    /** Пустая форма: ни названия, ни адреса, ни места. */
    @Test
    fun `пустая форма`() = look("place-add-empty", mockCabinet(EMPTY))

    /** Регистр не ответил вовсе: пустой шаг обязан сказать об этом. */
    @Test
    fun `регистр не ответил`() =
        look("place-add-registry-silent", mockCabinet(EMPTY, HttpStatusCode.BadGateway))

    /** Регистр ответил областями: с них начинается подбор адреса. */
    @Test
    fun `регистр ответил областями`() = look("place-add-regions", mockCabinet(REGIONS))

    /**
     * Снимок окна заведения точки.
     *
     * Владелец входит отметкой разработчика: без доступа окно не спросило
     * бы регистр вовсе, и отказных состояний на снимке не было бы.
     */
    private fun look(name: String, cabinet: CabinetSession) {
        val session = Look.session()
        RenderProbe(WIDE, HIGH) { AddPlaceCard(session, cabinet, Look.cabinet, onDismiss = {}, onAdded = {}) }
            .use { probe ->
                repeat(SETTLE) { probe.frame() }
                Look.shot(name, probe.frame())
            }
    }

    private companion object {
        const val EMPTY = """{"items":[]}"""

        const val REGIONS = """{"items":[
            {"id":1,"name":"Алматы","level":"REGION"},
            {"id":2,"name":"Астана","level":"REGION"},
            {"id":3,"name":"Карагандинская область","level":"REGION"}]}"""

        const val SETTLE = 24
        const val WIDE = 1180
        const val HIGH = 820
    }
}
