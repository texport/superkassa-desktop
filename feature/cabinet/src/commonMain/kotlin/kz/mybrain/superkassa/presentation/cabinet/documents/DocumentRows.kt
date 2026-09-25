package kz.mybrain.superkassa.presentation.cabinet.documents

import kotlinx.datetime.TimeZone
import kz.mybrain.superkassa.domain.cabinet.model.documents.DocumentKind
import kz.mybrain.superkassa.domain.cabinet.model.documents.DocumentPeriod
import kz.mybrain.superkassa.domain.cabinet.model.documents.DocumentsOverview
import kz.mybrain.superkassa.presentation.common.document.JournalEmpty
import kz.mybrain.superkassa.presentation.common.period.JournalPeriod
import kz.mybrain.superkassa.presentation.common.period.workplaceToday
import kz.mybrain.superkassa.strings.api.cabinet.CabinetTexts
import kotlin.time.Instant

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
internal fun cabinetPeriodOf(period: JournalPeriod, zone: TimeZone = TimeZone.currentSystemDefault()): DocumentPeriod {
    val window = period.range ?: return DocumentPeriod()
    val openEnded = window.to >= workplaceToday(zone)
    return DocumentPeriod(
        from = Instant.fromEpochMilliseconds(window.fromMillis(zone)).toString(),
        to = if (openEnded) null else Instant.fromEpochMilliseconds(window.toMillis(zone)).toString()
    )
}

/** Название вида документов на сегменте выбора. */
internal fun DocumentKind.title(texts: CabinetTexts): String = when (this) {
    DocumentKind.Receipts -> texts.receipts
    DocumentKind.Shifts -> texts.shifts
    DocumentKind.Reports -> texts.reports
    DocumentKind.CashMovements -> texts.cashMovements
}

/**
 * Чем объяснить пустой список документов.
 *
 * Над списком стоят счётчики за всё время, и «Здесь появится то, что БФД
 * приняла от этой кассы» рядом с сотней чеков читалось как потеря
 * документов: чеки у кассы есть, их нет за выбранную неделю. Названо это
 * по-разному, и во втором случае сказано, что делать.
 */
internal fun documentsEmpty(
    kind: DocumentKind,
    period: JournalPeriod,
    overview: DocumentsOverview?,
    texts: CabinetTexts
): JournalEmpty {
    val kept = overview?.let(kind.countIn) ?: 0
    val narrowed = kind.dated && period.range != null && kept > 0
    return if (narrowed) {
        JournalEmpty(texts.documentsNoneInPeriod, texts.hints.documentsNoneInPeriod)
    } else {
        JournalEmpty(texts.documentsEmpty, texts.hints.documentsEmpty)
    }
}
