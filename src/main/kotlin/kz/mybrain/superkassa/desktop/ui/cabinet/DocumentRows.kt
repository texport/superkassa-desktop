package kz.mybrain.superkassa.desktop.ui.cabinet

import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetShift
import kz.mybrain.superkassa.desktop.server.cabinet.DocumentPeriod
import kz.mybrain.superkassa.desktop.server.cabinet.ReceiptSearch
import kz.mybrain.superkassa.desktop.server.cabinet.cashMovements
import kz.mybrain.superkassa.desktop.server.cabinet.receipts
import kz.mybrain.superkassa.desktop.server.cabinet.reports
import kz.mybrain.superkassa.desktop.server.cabinet.shifts
import kz.mybrain.superkassa.desktop.ui.history.JournalPeriod
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Что стоит за строкой списка.
 *
 * У чека, отчёта и движения денег кабинет отдаёт содержимое отдельной
 * ручкой по идентификатору операции. У смены такой ручки нет: всё, что
 * о ней известно, приходит вместе со списком — поэтому смена и открывается
 * из того, что уже прочитано, а не повторным обращением.
 */
sealed interface RowTarget {
    data class Remote(val transactionId: String) : RowTarget
    data class Local(val shift: CabinetShift) : RowTarget
}

/** Прочитанная страница списка и сколько всего строк за сроком. */
data class DocumentSlice(val rows: List<CabinetDocumentRow> = emptyList(), val total: Long = 0)

/**
 * Читает страницу выбранного вида документов.
 *
 * Виды кабинет отдаёт разными списками, а показывается у них одно и то
 * же — когда документ пробит, чем он является, на сколько он и что с ним
 * стало. Поэтому страница приводится к строкам общего журнала здесь,
 * а не четырьмя похожими разметками на экране.
 */
suspend fun loadDocuments(
    cabinet: CabinetSession,
    token: String,
    id: String,
    kind: DocumentKind,
    page: Int,
    period: DocumentPeriod,
    texts: CabinetTexts
): DocumentSlice = when (kind) {
    DocumentKind.Receipts -> {
        val found = cabinet.client.receipts(
            token,
            id,
            ReceiptSearch(page = page, dateFrom = period.fromText(), dateTo = period.toText())
        )
        DocumentSlice(found.items.map { receiptRow(it, texts) }, found.totalElements)
    }

    DocumentKind.Shifts -> {
        val found = cabinet.client.shifts(token, id, page)
        DocumentSlice(found.items.map { shiftRow(it, texts) }, found.totalElements)
    }

    DocumentKind.Reports -> {
        val found = cabinet.client.reports(token, id, page, period)
        DocumentSlice(found.items.map { reportRow(it, texts) }, found.totalElements)
    }

    DocumentKind.CashMovements -> {
        val found = cabinet.client.cashMovements(token, id, page, period)
        DocumentSlice(found.items.map { movementRow(it, texts) }, found.totalElements)
    }
}

/**
 * Срок журнала отбором кабинета.
 *
 * Границы считаются от начала суток рабочего места, а не «минус
 * столько-то часов»: смена начинается утром, и «неделя» обязана включать
 * её целиком.
 *
 * У окна, кончающегося сегодня, верхней границы нет: чек, пробитый минуту
 * назад, обязан попасть и в «сегодня», и в «эту неделю». У перелистнутого
 * назад окна она есть — иначе «прошлая неделя» отдавала бы и эту.
 */
fun cabinetPeriodOf(period: JournalPeriod, zone: ZoneId = ZoneId.systemDefault()): DocumentPeriod {
    val window = period.range ?: return DocumentPeriod()
    return DocumentPeriod(
        from = Instant.ofEpochMilli(window.fromMillis(zone)),
        to = if (window.to < LocalDate.now(zone)) Instant.ofEpochMilli(window.toMillis(zone)) else null
    )
}

/**
 * Виды документов кассы в кабинете.
 *
 * Выбор вида решает, какой список читать у кабинета, — это источник
 * строк, а не отбор уже прочитанного. Поэтому виды стоят сегментами,
 * а плашки отбора над таблицей разделяют то, что внутри вида: продажу
 * от возврата, X-отчёт от Z-отчёта.
 *
 * @param dated отбирает ли кабинет этот вид по сроку.
 */
enum class DocumentKind(val title: (CabinetTexts) -> String, val dated: Boolean = true) {
    Receipts({ it.receipts }),
    Shifts({ it.shifts }, dated = false),
    Reports({ it.reports }),
    CashMovements({ it.cashMovements })
}
