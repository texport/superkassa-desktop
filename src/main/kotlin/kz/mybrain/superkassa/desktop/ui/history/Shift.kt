package kz.mybrain.superkassa.desktop.ui.history

import kotlinx.serialization.Serializable

/**
 * Смена в том виде, в каком её отдаёт узел.
 *
 * Поля необязательны намеренно: у открытой смены нет ни времени закрытия,
 * ни документа закрытия, и приложение обязано это переживать, а не падать
 * на разборе.
 */
@Serializable
data class Shift(
    val id: String,
    val shiftNo: Long? = null,
    val status: String? = null,
    val openedAt: Long? = null,
    val closedAt: Long? = null,
    val openDocumentId: String? = null,
    val closeDocumentId: String? = null
) {
    /** Закрытая смена: только у неё есть Z-отчёт. */
    val isClosed: Boolean get() = status == CLOSED

    /**
     * Документ закрытия смены — он же Z-отчёт.
     *
     * У открытой смены его нет, и кнопка печати у неё не показывается:
     * нажатие на неё дало бы отказ узла вместо отчёта.
     */
    val zReportId: String? get() = closeDocumentId?.takeIf { isClosed }
}

private const val CLOSED = "CLOSED"
