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
    val documentsInShift: String,
    val shiftDocuments: String,
    val refused: String,
    val refusedHint: String,
    val openShift: String,
    val xReport: String,
    val closeShift: String,
    /** Вопрос перед Z-отчётом: он не отменяется. */
    val closeShiftAsk: String,
    /** Что станет с итогами: подставляются число документов и остаток в ящике. */
    val closeShiftExplain: String,
    /** Остаток наличных уйдёт изъятием вместе с Z-отчётом. */
    val closeShiftCashout: String,
    /** Остаток наличных перейдёт в новую смену. */
    val closeShiftKeepsCash: String,
    val openShiftHint: String,
    val openShiftAdmin: String,
    val shiftOpened: String,
    val shiftClosedDone: String,
    val xReportDone: String,
    val printForm: String,
    /** Узел о смене не ответил: приложение не называет состояние за него. */
    val shiftUnknown: String,

    /**
     * За открытую смену документов ещё нет.
     *
     * Своё название списка, а не повтор заголовка над ним: на месте
     * содержимого стояло то же «Документы смены», а подсказкой — одно
     * слово «Чек», из которого кассир ничего не узнавал.
     */
    val shiftEmpty: String,
    val shiftEmptyHint: String,

    /**
     * Документы открытой смены узел не отдал.
     *
     * Отдельно от пустого списка: у кассы, снятой с учёта, узел отвечает
     * KKM_BLOCKED, и «Документов пока нет» обещало пустую смену там, где
     * список просто не прочитан.
     */
    val documentsUnread: String,
    val documentsUnreadHint: String,
    /**
     * Узел о смене не ответил, и документов поэтому не видно.
     *
     * Сказать здесь «смена закрыта» нельзя: плитка над списком в этот
     * момент пишет «Неизвестна», и две надписи спорили друг с другом.
     */
    val shiftUnknownHint: String,
    /** Подпись номера документа в строке списка. */
    val documentNo: String
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

    /**
     * Справочник не ответил: спросить о товаре сейчас нельзя.
     *
     * Отдельная строка, а не [barcodeMissing]: товар в справочнике
     * есть, и «такого штрихкода нет» отправляло кассира искать беду
     * с товаром вместо настоящей — потерянной связи.
     */
    val barcodeUnavailable: String,
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
    val deposit: String,
    val withdraw: String,
    val deposited: String,
    val withdrawn: String
)
