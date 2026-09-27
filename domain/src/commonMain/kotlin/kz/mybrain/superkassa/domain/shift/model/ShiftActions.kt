package kz.mybrain.superkassa.domain.shift.model

import io.github.texport.superkassa.core.presentation.api.model.kkm.FiscalDocumentResponse
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import kz.mybrain.superkassa.domain.document.model.hasOwnAmount
import kz.mybrain.superkassa.domain.document.model.refusedByOfd
import kz.mybrain.superkassa.domain.kkm.model.isProgramming

/**
 * Можно ли предложить действие со сменой: касса выбрана и не в режиме
 * программирования.
 *
 * В режиме программирования касса настраивается, а не торгует: открыть
 * или закрыть смену, снять отчёт она откажет, и кнопка, на которую
 * приходит отказ, кассиру ничего не объясняет.
 */
fun shiftActionsAllowed(kkm: KkmResponse?): Boolean = kkm != null && !kkm.isProgramming

/**
 * Можно ли предложить досылку накопленного с главного экрана.
 *
 * Касса ставит неудавшееся на повтор только администратору, при закрытой
 * смене и в режиме программирования. В остальное время накопленное уходит
 * само, как только вернётся связь, а кнопка приносила кассиру отказ
 * «нет прав» — при открытой смене она не могла сработать никогда.
 */
fun queueResendAllowed(kkm: KkmResponse?, isAdmin: Boolean, shift: ShiftState): Boolean =
    isAdmin && shift == ShiftState.Closed && kkm?.isProgramming == true

/**
 * Есть ли в смене из чего снять X-отчёт.
 *
 * Смену у БФД открывает не кнопка «Открыть смену» — такой команды в CPCR
 * нет, — а первый документ с суммой: чек или внесение. Пока его нет,
 * БФД на X-отчёт отвечает кодом 13 «накопленного отчёта нет», и кассир
 * получал отказ на пустом месте. Отвергнутый БФД документ смены у него
 * не открыл; пробитый без связи — фискальный и уйдёт первым в очереди.
 *
 * @param documents документы открытой смены.
 */
fun xReportAllowed(documents: List<FiscalDocumentResponse>): Boolean =
    documents.any { it.hasOwnAmount && !it.refusedByOfd }
