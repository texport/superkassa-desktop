package kz.mybrain.superkassa.desktop.ui.strings

/**
 * Надписи состояний и показа печатной формы.
 *
 * Словари состояний узла и подписи просмотра формы — то, что читают
 * на всех экранах, а не в одном разделе.
 */

data class PreviewStrings(
    val title: String,
    val narrower: String,
    val wider: String,
    val close: String,
    val missing: String,
    val print: String,
    val printSent: String,
    val printFailed: String,
    val save: String,
    val saved: String,
    val zoomIn: String,
    val zoomOut: String,
    val fit: String,
    /** Форму рисует касса: узлу нужен её пин, а кассир мог и не входить. */
    val drawPin: String,
    val drawPinHint: String,
    val draw: String,
    /** Узел не отдал ни одной кассы — рисовать форму нечем. */
    val noDrawer: String
)

data class StatusStrings(
    val delivered: String,
    val resent: String,
    val refused: String,
    val internal: String,
    val queued: String
)

data class EnumStrings(
    val vatNone: String,
    val vat0: String,
    val vat5: String,
    val vat10: String,
    val vat16: String,
    val paymentCash: String,
    val paymentCard: String,
    val paymentElectronic: String,
    val paymentMobile: String,
    val paymentCredit: String,
    val paymentTare: String,
    val domainTrading: String,
    val domainServices: String,
    val domainHotels: String,
    val domainGasOil: String,
    val domainTaxi: String,
    val domainParking: String,
    val docCheck: String,
    val docShiftOpen: String,
    val docShiftClose: String,
    val docCashIn: String,
    val docCashOut: String,
    val docReportX: String,
    val stateActive: String,
    val stateIdle: String,
    val stateBlocked: String,
    val stateProgramming: String,
    val stateRegistration: String
)
