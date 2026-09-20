package kz.mybrain.superkassa.desktop.ui.analytics

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.server.cabinet.SalesDay
import kz.mybrain.superkassa.desktop.server.cabinet.SalesDelivery
import kz.mybrain.superkassa.desktop.server.cabinet.SalesFilter
import kz.mybrain.superkassa.desktop.server.cabinet.SalesHour
import kz.mybrain.superkassa.desktop.server.cabinet.SalesSummary
import kz.mybrain.superkassa.desktop.server.cabinet.SalesUnit
import kz.mybrain.superkassa.desktop.server.cabinet.salesByCashRegister
import kz.mybrain.superkassa.desktop.server.cabinet.salesByDay
import kz.mybrain.superkassa.desktop.server.cabinet.salesByHour
import kz.mybrain.superkassa.desktop.server.cabinet.salesByRetailPlace
import kz.mybrain.superkassa.desktop.server.cabinet.salesDelivery
import kz.mybrain.superkassa.desktop.server.cabinet.salesSummary
import kz.mybrain.superkassa.desktop.ui.history.JOURNAL_DAY
import kz.mybrain.superkassa.desktop.ui.history.JournalPeriod
import kz.mybrain.superkassa.desktop.ui.history.JournalRange
import kz.mybrain.superkassa.desktop.ui.history.JournalSpan
import java.time.LocalDate

/**
 * Состояние торговой сводки.
 *
 * Шесть ручек кабинета спрашиваются разом и складываются в один [SalesView]:
 * показывать плитки за одну неделю, а таблицу под ними за другую нельзя,
 * поэтому срок меняет их все сразу, а частичный ответ не показывается
 * вовсе — помеха одна на весь экран.
 *
 * Срок по умолчанию — неделя: владелец открывает раздел, чтобы посмотреть,
 * как торговали на этой неделе, а не за один сегодняшний день.
 */
class AnalyticsSalesModel(private val cabinet: CabinetSession) {

    /** Срок сводки; выбирается той же полосой, что и срок журнала кассы. */
    var period: JournalPeriod by mutableStateOf(JournalPeriod.of(JournalSpan.Week))

    var view: SalesView? by mutableStateOf(null)
        private set

    var trouble: AnalyticsTrouble? by mutableStateOf(null)
        private set

    var loading: Boolean by mutableStateOf(false)
        private set

    /** Спрашивает кабинет обо всём сроке разом. */
    suspend fun load() {
        val token = cabinet.token ?: return
        loading = true
        trouble = null
        askedCabinet { ask(token, salesFilter(period)) }
            .onSuccess { view = it }
            .onFailure {
                view = null
                trouble = analyticsTrouble(it)
            }
        loading = false
    }

    /**
     * Шесть запросов разом.
     *
     * Подряд они заняли бы шесть кругов до кабинета вместо одного,
     * а первый же отказ отменяет остальные: половина сводки на экране
     * хуже честной надписи о том, что её нет.
     */
    private suspend fun ask(token: String, filter: SalesFilter): SalesView = coroutineScope {
        val summary = async { cabinet.client.salesSummary(token, filter) }
        val days = async { cabinet.client.salesByDay(token, filter) }
        val hours = async { cabinet.client.salesByHour(token, filter) }
        val registers = async { cabinet.client.salesByCashRegister(token, filter) }
        val places = async { cabinet.client.salesByRetailPlace(token, filter) }
        val delivery = async { cabinet.client.salesDelivery(token, filter) }
        SalesView(
            range = JournalRange(filter.from, filter.to),
            summary = summary.await(),
            days = days.await(),
            hours = hours.await(),
            registers = registers.await(),
            places = places.await(),
            delivery = delivery.await()
        )
    }
}

/**
 * Вся сводка за срок одним ответом.
 *
 * Границы срока лежат здесь же: ряд столбиков по дням достраивает пустые
 * сутки, а знать о них без границ неоткуда — кабинет присылает только
 * те сутки, в которые торговали.
 */
data class SalesView(
    val range: JournalRange,
    val summary: SalesSummary,
    val days: List<SalesDay>,
    val hours: List<SalesHour>,
    val registers: List<SalesUnit>,
    val places: List<SalesUnit>,
    val delivery: SalesDelivery
) {
    /**
     * Ни одного чека за срок.
     *
     * Считается по числам, а не по длине списков: кабинет присылает
     * только непустые сутки, и месяц без единого документа приходит
     * пустым списком — как и месяц, ответ по которому ещё не разобран.
     *
     * Покупка у населения считается наравне с продажей: срок, в который
     * касса только скупала, документы за собой оставил, и надпись
     * «документов нет» над ними была бы неправдой.
     */
    val empty: Boolean
        get() = summary.receiptCount == 0 &&
            !summary.purchased &&
            days.none { it.receiptCount > 0 } &&
            registers.none { it.receiptCount > 0 }
}

/**
 * Границы срока для сводки.
 *
 * У «всего времени» границ нет, а ручкам сводки они обязательны: сводка
 * без границ — это вся история компании. Тогда берётся последний год,
 * и эти границы показываются над плитками датами, чтобы владелец видел,
 * за что посчитано.
 */
fun salesRange(period: JournalPeriod, today: LocalDate = LocalDate.now()): JournalRange =
    period.range ?: JournalRange(today.minusYears(1).plusDays(1), today)

/** Отбор кабинета по выбранному сроку. */
fun salesFilter(period: JournalPeriod, today: LocalDate = LocalDate.now()): SalesFilter =
    salesRange(period, today).let { SalesFilter(from = it.from, to = it.to) }

/** Срок сводки словами: всегда датами, в том числе и у «всего времени». */
fun salesRangeText(period: JournalPeriod, today: LocalDate = LocalDate.now()): String =
    salesRange(period, today).let { "${JOURNAL_DAY.format(it.from)} — ${JOURNAL_DAY.format(it.to)}" }
