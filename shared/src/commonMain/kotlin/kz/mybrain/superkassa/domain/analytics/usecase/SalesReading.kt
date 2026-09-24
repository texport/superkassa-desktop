package kz.mybrain.superkassa.domain.analytics.usecase

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.LocalDate
import kz.mybrain.superkassa.domain.analytics.model.AnalyticsAnswer
import kz.mybrain.superkassa.domain.analytics.model.PlaceAddress
import kz.mybrain.superkassa.domain.analytics.model.SalesSpan
import kz.mybrain.superkassa.domain.analytics.model.SalesView
import kz.mybrain.superkassa.domain.analytics.model.valueOrNull
import kz.mybrain.superkassa.domain.analytics.port.Analytics

/**
 * Сводка срока из трёх обращений, где беда двух не отменяет главного.
 *
 * Главное — разрезы срока: без них сводки нет. Прошлый срок нужен только
 * сравнению, и его отказ сводку не роняет: числа стоят без изменения
 * к прошлому сроку, а не весь экран без чисел. Срок, который ещё идёт,
 * с прошлым не сравнивается вовсе — см. [SalesSpan.finished].
 *
 * @param register касса отбора; `null` — вся сеть.
 * @param places торговые точки для свода по регионам; пусто — свода нет.
 */
internal suspend fun readSpan(
    analytics: Analytics,
    span: SalesSpan,
    today: LocalDate,
    register: String?,
    places: suspend () -> List<PlaceAddress>
): AnalyticsAnswer<SalesView> = coroutineScope {
    val running = !span.finished(today)
    val figures = async { analytics.sales(span.filter(register)) }
    val before = async { if (running) null else analytics.summary(span.previous().filter(register)).valueOrNull() }
    val catalogue = async { places() }
    when (val answer = figures.await()) {
        is AnalyticsAnswer.Troubled -> answer
        is AnalyticsAnswer.Done -> AnalyticsAnswer.Done(
            SalesView.of(span, answer.value)
                .copy(previous = before.await(), running = running, retailPlaces = catalogue.await())
        )
    }
}
