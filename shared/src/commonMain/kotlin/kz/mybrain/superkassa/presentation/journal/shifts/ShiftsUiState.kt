package kz.mybrain.superkassa.presentation.journal.shifts

import io.github.texport.superkassa.core.presentation.api.model.kkm.FiscalDocumentResponse
import io.github.texport.superkassa.core.presentation.api.model.reference.TrilingualMessageResponse
import io.github.texport.superkassa.core.presentation.api.model.shift.ShiftResponse
import kz.mybrain.superkassa.presentation.journal.PageOutcome

/**
 * Прошлые смены кассы и документы открытой из них.
 *
 * @property page чем кончилось чтение смен: прочитаны ли и есть ли ещё.
 * @property loading смены читаются. Стоит с первого кадра: иначе пустой
 *   список успевал мелькнуть объяснением пустоты.
 * @property opened смена, документы которой открыты; `null` — список смен.
 * @property documentsPage чем кончилось чтение документов смены: касса,
 *   которая отказала или промолчала, о них не сказала ничего, и «документов
 *   нет» про смену с сотней чеков — неправда.
 */
data class ShiftsUiState(
    val shifts: List<ShiftResponse> = emptyList(),
    val page: PageOutcome = PageOutcome.unread,
    val loading: Boolean = true,
    val opened: ShiftResponse? = null,
    val documents: List<FiscalDocumentResponse> = emptyList(),
    val documentsPage: PageOutcome = PageOutcome.unread,
    val opening: Boolean = false,
    val documentTypes: Map<String, TrilingualMessageResponse> = emptyMap()
)

/**
 * Что кассир может сделать со сменами.
 *
 * Действия по умолчанию пустые — для снимков вида, где нажимать некому.
 */
interface ShiftsActions {
    fun reload() = Unit

    fun more() = Unit

    /** Открывает документы смены; `null` — возврат к списку смен. */
    fun open(shift: ShiftResponse?) = Unit

    fun moreDocuments() = Unit
}
