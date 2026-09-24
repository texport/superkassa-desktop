package kz.mybrain.superkassa.domain.shift.model

import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
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
