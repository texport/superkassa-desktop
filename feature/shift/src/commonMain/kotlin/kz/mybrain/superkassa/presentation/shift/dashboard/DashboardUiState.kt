package kz.mybrain.superkassa.presentation.shift.dashboard

import io.github.texport.superkassa.core.presentation.api.model.kkm.FiscalDocumentResponse
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import io.github.texport.superkassa.core.presentation.api.model.reference.TrilingualMessageResponse
import io.github.texport.superkassa.core.presentation.api.model.shift.ShiftResponse
import kz.mybrain.superkassa.domain.document.model.printable
import kz.mybrain.superkassa.domain.kkm.model.isBlocked
import kz.mybrain.superkassa.domain.kkm.model.isProgramming
import kz.mybrain.superkassa.domain.shift.model.ShiftState
import kz.mybrain.superkassa.domain.shift.model.queueResendAllowed
import kz.mybrain.superkassa.domain.shift.model.shiftActionsAllowed
import kz.mybrain.superkassa.domain.shift.model.xReportAllowed
import kz.mybrain.superkassa.domain.shift.model.zReportId
import kz.mybrain.superkassa.strings.api.Language

/**
 * Главный экран: выбранная касса, её смена и документы смены.
 *
 * @property shift состояние смены со слов кассы; пока касса не ответила — неизвестно.
 * @property shiftNumber номер открытой смены или последней закрытой.
 * @property dayLimitAt до какого момента касса примет чеки открытой смены:
 *   сутки от первого платёжного документа, по часам кассы; `null` —
 *   платёжных документов ещё нет.
 * @property dayLimitExceeded предел пройден: касса откажет в чеке, возврате
 *   и деньгах, пока смену не закроют.
 * @property documentsRead отдала ли касса документы открытой смены: пустой
 *   список и непрочитанный — разные вещи, по нулю кассир решает, снимать ли Z-отчёт.
 * @property cashInDrawer наличные в ящике, в тиынах: счётчик всей кассы, а не смены.
 * @property documentTypes названия видов документов на трёх языках: ключ — код вида.
 * @property operators кто оформил отклонённые документы: ключ — документ.
 * @property reading касса перечитывается.
 * @property busy идёт действие со сменой: кнопки не принимают второго нажатия.
 * @property lastClosed последняя закрытая смена, пока новая не открыта.
 */
data class DashboardUiState(
    val kkm: KkmResponse? = null,
    val isAdmin: Boolean = false,
    val shift: ShiftState = ShiftState.Unknown,
    val shiftNumber: Long? = null,
    val dayLimitAt: Long? = null,
    val dayLimitExceeded: Boolean = false,
    val documents: List<FiscalDocumentResponse> = emptyList(),
    val documentsRead: Boolean = false,
    val cashInDrawer: Long? = null,
    val documentTypes: Map<String, TrilingualMessageResponse> = emptyMap(),
    val operators: Map<String, String> = emptyMap(),
    val reading: Boolean = false,
    val busy: Boolean = false,
    val lastClosed: ShiftResponse? = null
) {
    /** Документы смены, которые ОФД отверг. */
    val refused: List<FiscalDocumentResponse> get() = documents.filterNot { it.printable }

    /** Заблокированная касса фискальных команд не принимает: кнопок ей нет. */
    val blocked: Boolean get() = kkm?.isBlocked == true

    val programming: Boolean get() = kkm?.isProgramming == true

    /** Касса назвала состояние смены: неизвестное — не повод предлагать действие. */
    val known: Boolean get() = shift != ShiftState.Unknown

    /** Действие со сменой можно предложить: касса выбрана, свободна и не в программировании. */
    val canAct: Boolean get() = !busy && shiftActionsAllowed(kkm)

    /** X-отчёт снимается, когда в смене есть документ с суммой: иначе БФД его не примет. */
    val canTakeXReport: Boolean get() = canAct && xReportAllowed(documents)

    /**
     * Z-отчёт, который кассир открывает с главного экрана: смена закрыта
     * и отчёт у неё есть. Сразу после закрытия его иначе приходилось
     * искать в журнале по сменам.
     */
    val zReportShift: ShiftResponse? get() = lastClosed?.takeIf { shift == ShiftState.Closed && it.zReportId != null }

    /** Досылку накопленного касса сейчас примет: иначе кнопки нет, а её условия названы строкой. */
    val canSendQueued: Boolean get() = queueResendAllowed(kkm, isAdmin, shift)

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
