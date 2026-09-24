package kz.mybrain.superkassa.presentation.journal.documents

import io.github.texport.superkassa.core.presentation.api.model.kkm.FiscalDocumentResponse
import io.github.texport.superkassa.core.presentation.api.model.reference.TrilingualMessageResponse
import kotlinx.datetime.LocalDate
import kz.mybrain.superkassa.presentation.common.document.JournalQuery
import kz.mybrain.superkassa.presentation.common.period.JournalPeriod
import kz.mybrain.superkassa.presentation.common.period.JournalSpan
import kz.mybrain.superkassa.presentation.common.period.dayRange
import kz.mybrain.superkassa.presentation.common.period.workplaceToday
import kz.mybrain.superkassa.presentation.journal.PageOutcome
import kz.mybrain.superkassa.strings.api.journal.HistoryJournalTexts

/**
 * Журнал документов кассы, каким его видит кассир.
 *
 * Живёт в модели окна: срок, отбор и прочитанные страницы переживают уход
 * кассира в продажу и обратно — прежде журнал открывался заново с сегодняшнего
 * дня и без отбора.
 *
 * @property view взгляд на журнал: по сроку или по сменам.
 * @property documents прочитанные документы срока, как их отдала касса.
 * @property page чем кончилось чтение: прочитан ли срок и есть ли ещё.
 *   Прежде журнал брал первые двести документов и молчал об остальных,
 *   а неудачу чтения выдавал за пустой срок.
 * @property loading срок читается. Стоит с первого кадра: иначе пустой
 *   список успевал мелькнуть объяснением пустоты.
 * @property delivery открытый чек и его доставка покупателю; `null` — окно закрыто.
 */
data class JournalUiState(
    val view: HistoryView = HistoryView.Day,
    val period: JournalPeriod = JournalPeriod.of(JournalSpan.Day),
    val query: JournalQuery = JournalQuery(),
    val documents: List<FiscalDocumentResponse> = emptyList(),
    val page: PageOutcome = PageOutcome.unread,
    val loading: Boolean = true,
    val documentTypes: Map<String, TrilingualMessageResponse> = emptyMap(),
    val delivery: ReceiptDeliveryUi? = null
) {
    /**
     * Границы срока для кассы: от начала первых суток до начала следующих.
     *
     * Срок без границ читается с начала счёта времени и до конца
     * сегодняшнего дня.
     */
    fun bounds(today: LocalDate = workplaceToday()): LongRange =
        (period.range?.fromMillis() ?: FIRST_RECORD)..(period.range?.toMillis() ?: dayRange(today).toMillis)
}

/** Взгляд на журнал: по дню или по сменам. */
enum class HistoryView(val title: (HistoryJournalTexts) -> String) {
    Day({ it.byPeriod }),
    Shifts({ it.byShift })
}

/**
 * Что кассир может сделать с журналом.
 *
 * Действия по умолчанию пустые — для снимков вида, где нажимать некому.
 */
interface JournalActions {
    fun show(view: HistoryView) = Unit

    fun choose(period: JournalPeriod) = Unit

    fun filter(query: JournalQuery) = Unit

    /** Дочитывает срок; после неудачи — повторяет чтение с того же места. */
    fun more() = Unit

    /** Открывает доставку чека [key] покупателю; у документа, который не чек, окна нет. */
    fun open(key: String) = Unit

    /** Отправляет чек ещё раз — туда, куда доставка не удалась. */
    fun resend() = Unit

    fun closeDelivery() = Unit
}

/** С какого момента читается срок без границ: с начала счёта времени. */
private const val FIRST_RECORD = 0L
