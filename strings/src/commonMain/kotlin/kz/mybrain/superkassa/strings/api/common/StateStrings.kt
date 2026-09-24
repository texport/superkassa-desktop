package kz.mybrain.superkassa.strings.api.common

import kz.mybrain.superkassa.strings.impl.common.documentName
import kz.mybrain.superkassa.strings.impl.common.paymentName

/*
 * Надписи состояний и показа печатной формы.
 *
 * Словари состояний узла и подписи просмотра формы — то, что читают
 * на всех экранах, а не в одном разделе.
 */

/** Надписи просмотра и печати печатной формы. */
data class PreviewStrings(
    val title: String,
    val narrower: String,
    val wider: String,
    val close: String,
    val missing: String,
    val print: String,
    val printSent: String,
    val printFailed: String,
    /** Печатать некуда: на машине нет ни одного принтера. */
    val printerMissing: String,
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

/** Состояния доставки документа в БФД. */
data class StatusStrings(
    val delivered: String,
    val resent: String,
    val refused: String,
    val internal: String,
    val queued: String,
    /**
     * Код состояния доставки пришёл, а разобрать его нечем.
     *
     * Поле состояния в спецификации узла — свободная строка, и незнакомый
     * код доходил до экрана кассира кодом.
     */
    val unknown: String
)

/** Названия значений справочников: ставки НДС, виды оплаты, отрасли, типы документов, состояния кассы. */
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
    /** Виды отрасли: их называет настройка кассы и заголовок её реквизитов. */
    val domainTrading: String,
    val domainServices: String,
    val domainHotels: String,
    val domainGasOil: String,
    val domainTaxi: String,
    val domainParking: String,
    val docCheck: String,
    val docSale: String,
    val docReturn: String,
    val docBuy: String,
    val docBuyReturn: String,
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
) {
    /**
     * Название вида оплаты, когда узел его не прислал.
     *
     * В справочнике узла у оплаты в кредит и тарой названий нет — они
     * объявлены устаревшими. Показывать кассиру голый код нельзя: он должен
     * читать чек, а не протокол.
     *
     * @return название; `null` — код незнаком.
     */
    fun paymentFallback(code: String): String? = paymentName(this, code)

    /**
     * Название типа документа, когда справочник узла его не знает.
     *
     * В журнале остались записи прежних версий: чеки под общим «CHECK»
     * и X-отчёт под «REPORT_X», которого в справочнике нет. Показывать
     * кассиру код нельзя даже для старой записи.
     *
     * @return название; `null` — код незнаком.
     */
    fun documentFallback(code: String): String? = documentName(this, code)
}
