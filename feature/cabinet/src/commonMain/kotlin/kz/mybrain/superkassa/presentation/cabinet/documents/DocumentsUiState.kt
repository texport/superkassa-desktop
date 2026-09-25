package kz.mybrain.superkassa.presentation.cabinet.documents

import kz.mybrain.superkassa.domain.cabinet.model.documents.DocumentKind
import kz.mybrain.superkassa.domain.cabinet.model.documents.DocumentsOverview
import kz.mybrain.superkassa.domain.cabinet.model.documents.OpenedDocument
import kz.mybrain.superkassa.domain.cabinet.model.documents.RowTarget
import kz.mybrain.superkassa.presentation.common.document.JournalQuery
import kz.mybrain.superkassa.presentation.common.period.JournalPeriod
import kz.mybrain.superkassa.presentation.common.period.JournalSpan

/**
 * Документы кассы по данным ОФД.
 *
 * Кабинет отдаёт список страницами по пятьдесят строк; прочитанное лежит
 * здесь вместе с тем, сколько строк всего за сроком, — без второго числа
 * кнопка «показать ещё» не знает, когда ей исчезнуть. Смена вида
 * документов или срока начинается с чистого листа.
 *
 * @property overview счётчики кассы за всё время: по ним пустой список
 *   отличает «этого у кассы нет» от «нет за срок».
 * @property page сколько страниц прочитано: отказ кабинета счётчик
 *   не двигает, и повтор берёт ту же страницу, а не пропускает её.
 * @property trouble почему страница не прочитана, словами владельца:
 *   о кассе, пробившей тысячу чеков, «документов нет» говорит, что чеки
 *   потеряны.
 * @property opened раскрытый документ: показывается вместо списка.
 * @property opening документ читается: нажатие на строку не должно
 *   оставаться без ответа.
 */
internal data class DocumentsUiState(
    val registerId: String? = null,
    val kind: DocumentKind = DocumentKind.Receipts,
    val period: JournalPeriod = JournalPeriod.of(JournalSpan.Week),
    val query: JournalQuery = JournalQuery(),
    val overview: DocumentsOverview? = null,
    val rows: List<CabinetDocumentRow> = emptyList(),
    val total: Long = 0,
    val page: Int = 0,
    val loading: Boolean = false,
    val trouble: String? = null,
    val opened: OpenedDocument? = null,
    val opening: Boolean = false
) {
    /** Есть ли ещё непрочитанные строки. */
    val hasMore: Boolean get() = rows.size < total

    /** Что стоит за строкой журнала: у чека — операция, у смены — сама смена. */
    fun targetOf(key: String): RowTarget? = rows.firstOrNull { it.entry.key == key }?.target
}
