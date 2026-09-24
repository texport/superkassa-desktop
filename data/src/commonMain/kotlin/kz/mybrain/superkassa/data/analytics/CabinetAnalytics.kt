package kz.mybrain.superkassa.data.analytics

import kotlinx.coroutines.CancellationException
import kz.mybrain.superkassa.domain.analytics.model.AnalyticsAnswer
import kz.mybrain.superkassa.domain.analytics.model.AnalyticsTrouble
import kz.mybrain.superkassa.domain.analytics.model.ExchangeAddresses
import kz.mybrain.superkassa.domain.analytics.model.KkmMapView
import kz.mybrain.superkassa.domain.analytics.model.PlaceAddress
import kz.mybrain.superkassa.domain.analytics.model.PositionSource
import kz.mybrain.superkassa.domain.analytics.model.SalesFigures
import kz.mybrain.superkassa.domain.analytics.model.SalesFilter
import kz.mybrain.superkassa.domain.analytics.model.SalesSummary
import kz.mybrain.superkassa.domain.analytics.port.Analytics
import kz.mybrain.superkassa.domain.log.port.Journal
import kz.mybrain.superkassa.integrations.bfdcabinet.BfdCabinet
import kz.mybrain.superkassa.integrations.bfdcabinet.CabinetExpired
import kz.mybrain.superkassa.integrations.bfdcabinet.CabinetRefusal

/**
 * Аналитика — ручками кабинета БФД через его модуль.
 *
 * Кабинет один на приложение: вошедший владелец у аналитики и у разделов
 * кабинета общий, и доступ к ручкам модуль держит сам.
 *
 * Неудача здесь не всплывает исключением, а становится помехой на самом
 * экране: `404` означает не отказ, а то, что раздел кабинета ещё
 * не выложен, и сказать об этом нужно словами раздела.
 *
 * @param journal куда писать неудачи: имя помехи, а не ответ — адреса
 *   и суммы владельца в файл для поддержки не пишутся.
 */
class CabinetAnalytics(private val cabinet: BfdCabinet, private val journal: Journal) : Analytics {

    override suspend fun kkms(source: PositionSource): AnalyticsAnswer<KkmMapView> =
        asked("analytics map") { cabinet.analytics.map(source.wire()).domain() }

    override suspend fun exchange(): AnalyticsAnswer<ExchangeAddresses> =
        asked("analytics addresses") { cabinet.analytics.exchangeAddresses().domain() }

    /** Шесть разрезов разом: первый же отказ отменяет остальные — частичной сводки нет. */
    override suspend fun sales(filter: SalesFilter): AnalyticsAnswer<SalesFigures> =
        asked("analytics sales") { cabinet.analytics.sales(filter.wire()).domain() }

    override suspend fun summary(filter: SalesFilter): AnalyticsAnswer<SalesSummary> =
        asked("analytics summary") { cabinet.analytics.summary(filter.wire()).domain() }

    override suspend fun places(): AnalyticsAnswer<List<PlaceAddress>> =
        asked("analytics places") { cabinet.places.all().map { PlaceAddress(it.id, it.name, it.address) } }

    /**
     * Обращение, из которого экран не выпадает исключением.
     *
     * Отмена помехой не считается и уходит дальше: закрытый раздел или
     * сменённый источник положения не должны оставлять за собой надпись
     * «кабинет не отвечает».
     */
    private suspend fun <T> asked(what: String, block: suspend () -> T): AnalyticsAnswer<T> {
        val outcome = runCatching { block() }
        val failure = outcome.exceptionOrNull() ?: return AnalyticsAnswer.Done(outcome.getOrThrow())
        if (failure is CancellationException || failure is Error) throw failure
        journal.warn("$what: ${failure::class.simpleName}")
        return AnalyticsAnswer.Troubled(troubleOf(failure))
    }
}

/**
 * Помеха по тому, чем кончилось обращение.
 *
 * `404` здесь не «не найдено», а «раздела ещё нет»: своих ресурсов,
 * которых можно не найти, у аналитики нет — она отвечает пустыми
 * списками и нулями даже по кассе без единого обмена. Конец доступа —
 * войти заново. Имя исключения владельцу ничего не объясняет: всё прочее
 * на экране — «кабинет не отвечает».
 */
internal fun troubleOf(failure: Throwable): AnalyticsTrouble = when {
    failure is CabinetRefusal && failure.httpStatus == NOT_FOUND -> AnalyticsTrouble.NotDeployed
    failure is CabinetRefusal -> AnalyticsTrouble.Refused(failure.text)
    failure is CabinetExpired -> AnalyticsTrouble.SignedOut
    else -> AnalyticsTrouble.Unreachable
}

private const val NOT_FOUND = 404
