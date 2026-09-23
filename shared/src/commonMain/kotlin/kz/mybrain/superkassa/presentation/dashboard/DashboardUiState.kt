package kz.mybrain.superkassa.presentation.dashboard

import io.github.texport.superkassa.core.presentation.api.model.kkm.FiscalDocumentResponse
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import io.github.texport.superkassa.core.presentation.api.model.reference.TrilingualMessageResponse
import kz.mybrain.superkassa.domain.document.printable
import kz.mybrain.superkassa.domain.kkm.isBlocked
import kz.mybrain.superkassa.domain.kkm.isProgramming
import kz.mybrain.superkassa.domain.shift.ShiftState
import kz.mybrain.superkassa.presentation.strings.Language

/**
 * Главный экран: выбранная касса, её смена и документы смены.
 *
 * @property shift состояние смены со слов кассы; пока касса не ответила — неизвестно.
 * @property shiftNumber номер открытой смены или последней закрытой.
 * @property shiftOpenedAt когда касса открыла смену; по суткам открытой смены
 *   касса блокируется.
 * @property documentsRead отдала ли касса документы открытой смены: пустой
 *   список и непрочитанный — разные вещи, по нулю кассир решает, снимать ли Z-отчёт.
 * @property cashInDrawer наличные в ящике, в тиынах: счётчик всей кассы, а не смены.
 * @property documentTypes названия видов документов на трёх языках: ключ — код вида.
 * @property operators кто оформил отклонённые документы: ключ — документ.
 * @property reading касса перечитывается.
 * @property busy идёт действие со сменой: кнопки не принимают второго нажатия.
 */
data class DashboardUiState(
    val kkm: KkmResponse? = null,
    val isAdmin: Boolean = false,
    val shift: ShiftState = ShiftState.Unknown,
    val shiftNumber: Long? = null,
    val shiftOpenedAt: Long? = null,
    val documents: List<FiscalDocumentResponse> = emptyList(),
    val documentsRead: Boolean = false,
    val cashInDrawer: Long? = null,
    val documentTypes: Map<String, TrilingualMessageResponse> = emptyMap(),
    val operators: Map<String, String> = emptyMap(),
    val reading: Boolean = false,
    val busy: Boolean = false
) {
    /** Документы смены, которые ОФД отверг. */
    val refused: List<FiscalDocumentResponse> get() = documents.filterNot { it.printable }

    /** Заблокированная касса фискальных команд не принимает: кнопок ей нет. */
    val blocked: Boolean get() = kkm?.isBlocked == true

    val programming: Boolean get() = kkm?.isProgramming == true

    /** Касса назвала состояние смены: неизвестное — не повод предлагать действие. */
    val known: Boolean get() = shift != ShiftState.Unknown

    /** Действие со сменой можно предложить: касса выбрана, свободна и не в программировании. */
    val canAct: Boolean get() = !busy && kkm != null && !programming

    /** Название вида документа из справочника кассы; `null` — справочник его не знает. */
    fun typeTitle(docType: String, language: Language): String? =
        documentTypes[docType]?.let {
            when (language) {
                Language.Ru -> it.ru
                Language.Kk -> it.kk
                Language.En -> it.en
            }
        }
}
