package kz.mybrain.superkassa.domain.analytics.usecase

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kz.mybrain.superkassa.domain.analytics.model.AnalyticsAnswer
import kz.mybrain.superkassa.domain.analytics.model.PlaceAddress
import kz.mybrain.superkassa.domain.analytics.model.SalesSpan
import kz.mybrain.superkassa.domain.analytics.model.SalesView
import kz.mybrain.superkassa.domain.analytics.model.valueOrNull
import kz.mybrain.superkassa.domain.analytics.port.Analytics

/**
 * Сводка срока из нескольких обращений, где беда прочих не отменяет главного.
 *
 * Главное — разрезы срока: без них сводки нет. Прошлый срок нужен только
 * сравнению, торговые точки — своду по регионам, кассы — числу открытых
 * смен; отказ любого из них сводку не роняет: на экране нет только его
 * части, а не всех чисел разом.
 *
 * Прошлый срок — отрезок той же длины вплотную перед выбранным
 * ([SalesSpan.previous]): сегодня сравнивается со вчера, эта неделя —
 * с прошлой. Срок, который ещё идёт, сравнивается так же: без сравнения
 * сводка по умолчанию — неделя по сегодня — не показывала его вовсе.
 *
 * @param register касса отбора; `null` — вся сеть.
 * @param places торговые точки для свода по регионам; пусто — свода нет.
 * @param shifts открытых смен в сети; `null` — не спрашиваются или кабинет не ответил.
 */
internal suspend fun readSpan(
    analytics: Analytics,
    span: SalesSpan,
    register: String?,
    places: suspend () -> List<PlaceAddress>,
    shifts: suspend () -> Int?
): AnalyticsAnswer<SalesView> = coroutineScope {
    val figures = async { analytics.sales(span.filter(register)) }
    val before = async { analytics.summary(span.previous().filter(register)).valueOrNull() }
    val catalogue = async { places() }
    val open = async { shifts() }
    when (val answer = figures.await()) {
        is AnalyticsAnswer.Troubled -> answer
        is AnalyticsAnswer.Done -> AnalyticsAnswer.Done(
            SalesView.of(span, answer.value)
                .copy(previous = before.await(), retailPlaces = catalogue.await(), openShifts = open.await())
        )
    }
}
