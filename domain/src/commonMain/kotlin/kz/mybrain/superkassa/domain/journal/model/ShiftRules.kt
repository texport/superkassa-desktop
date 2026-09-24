package kz.mybrain.superkassa.domain.journal.model

import io.github.texport.superkassa.core.presentation.api.model.shift.ShiftResponse
import io.github.texport.superkassa.core.presentation.api.model.shift.ShiftStatus

/**
 * Правила о смене, какой её отдаёт ядро.
 *
 * Своей копии смены у приложения нет: журнал прошлых смен читает ответ
 * фасада как есть, а вопросы к нему собраны здесь.
 */

/** Закрытая смена: только у неё есть Z-отчёт. */
val ShiftResponse.isClosed: Boolean
    get() = status == ShiftStatus.CLOSED

/**
 * Документ закрытия смены — он же Z-отчёт.
 *
 * У открытой смены его нет, и кнопка печати у неё не показывается:
 * нажатие на неё дало бы отказ кассы вместо отчёта.
 */
val ShiftResponse.zReportId: String?
    get() = closeDocumentId?.takeIf { isClosed }
