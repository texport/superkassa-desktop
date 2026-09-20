package kz.mybrain.superkassa.desktop.ui.strings

/**
 * Надписи рабочих экранов кассира.
 *
 * Продажа, возврат, деньги и журнал — то, чем кассир занят всю смену.
 * Вынесены отдельно от служебных: править их приходится вместе, а искать
 * среди девяноста строк настроек — нет.
 */

data class AutonomousStrings(
    val title: String,
    val explain: String,
    val waiting: String,
    val checkLink: String,
    val sendQueued: String,
    val sendQueuedRules: String,
    val linkBack: String
)

data class DashboardStrings(
    val state: String,
    val shift: String,
    val shiftOpenNo: String,
    val shiftClosed: String,
    val cashInDrawer: String,
    val documentsInShift: String,
    val shiftDocuments: String,
    val refused: String,
    val refusedHint: String,
    val openShift: String,
    val xReport: String,
    val closeShift: String,
    val openShiftHint: String,
    val openShiftAdmin: String,
    val shiftOpened: String,
    val shiftClosedDone: String,
    val xReportDone: String,
    val printForm: String,
    /** Узел о смене не ответил: приложение не называет состояние за него. */
    val shiftUnknown: String,
    /** Почему кассе, снятой с учёта, не предлагают действий со сменой. */
    val blockedNoActions: String
)

data class SaleStrings(
    val receipt: String,
    val sale: String,
    val purchase: String,
    val issueSale: String,
    val issuePurchase: String,
    val issuing: String,
    val name: String,
    val price: String,
    val quantity: String,
    val measureUnit: String,
    val vat: String,
    val discount: String,
    val storno: String,

    /** Снять отметку сторно со строки: пока чек не пробит, она черновая. */
    val stornoUndo: String,
    val add: String,
    val remove: String,
    val clearBasket: String,
    val receiptDiscount: String,
    val receiptMarkup: String,
    val discountOrMarkup: String,
    val customerBin: String,
    val taken: String,
    val total: String,
    val domainHint: String,
    val barcode: String,
    val barcodeSearch: String,
    val barcodeSearching: String,
    val barcodeFind: String,
    val barcodeMissing: String,
    val domainKind: String,
    val accountNumber: String,
    val cardNumber: String,
    val carNumber: String,
    val fee: String,
    val byOrder: String,
    val parkingHours: String,
    val payment: String,
    val delivered: String,
    val queued: String,
    val deliveryState: String
)

data class ReturnStrings(
    val title: String,
    val empty: String,
    val receiptNo: String,
    val refundFor: String,
    val saleReturn: String,
    val giveBack: String,
    val purchaseReturn: String,
    val takeBack: String,
    val done: String
)

data class CashStrings(
    val title: String,
    val inDrawer: String,
    val deposit: String,
    val withdraw: String,
    val deposited: String,
    val withdrawn: String
)

data class HistoryStrings(
    val title: String,
    val load: String,
    val loading: String
)
