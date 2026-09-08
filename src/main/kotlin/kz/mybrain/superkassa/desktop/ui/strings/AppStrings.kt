package kz.mybrain.superkassa.desktop.ui.strings

/**
 * Язык интерфейса.
 *
 * Казахский стоит первым: это государственный язык, и касса работает
 * в Казахстане. Название каждого языка написано на нём самом — так его
 * узнают, не зная двух других.
 */
enum class Language(val code: String, val title: String) {
    Kk("kk", "Қазақша"),
    Ru("ru", "Русский"),
    En("en", "English");

    companion object {
        fun byCode(code: String?): Language = entries.firstOrNull { it.code == code } ?: Kk
    }
}

/**
 * Все надписи кассы в одном месте.
 *
 * Строка объявляется один раз и берётся по смыслу. Разложено по экранам:
 * так видно, что перевод полон, а компилятор не даст забыть язык — новое
 * поле обязано появиться во всех трёх.
 */
data class AppStrings(
    val common: CommonStrings,
    val login: LoginStrings,
    val shell: ShellStrings,
    val sections: SectionStrings,
    val dashboard: DashboardStrings,
    val autonomous: AutonomousStrings,
    val sale: SaleStrings,
    val returns: ReturnStrings,
    val cash: CashStrings,
    val history: HistoryStrings,
    val queue: QueueStrings,
    val users: UserStrings,
    val settings: SettingStrings,
    val preview: PreviewStrings,
    val status: StatusStrings,
    val enums: EnumStrings
)

data class CommonStrings(
    val refresh: String,
    val hide: String,
    val print: String,
    val amount: String,
    val pin: String,
    val loading: String,
    val nodeOnline: String,
    val nodeOffline: String,
    val nodeUnavailable: String,
    val refusalCode: String,
    val deliveredToOfd: String,
    val queuedNoLink: String,
    val deliveryState: String,
    val collapse: String,
    val explain: String,
    val expand: String
)

data class LoginStrings(
    val title: String,
    val search: String,
    val noKkms: String,
    val yourKkm: String,
    val pick: String,
    val picked: String,
    val enter: String,
    val reload: String,
    val noKkmChosen: String,
    val pickHint: String,
    val factory: String
)

data class ShellStrings(
    val noKkm: String,
    val autonomous: String,
    val blocked: String,
    val changeCashier: String
)

data class SectionStrings(
    val dashboard: String,
    val sale: String,
    val returns: String,
    val cash: String,
    val history: String,
    val queue: String,
    val users: String,
    val settings: String,
    val register: String,
    val cabinet: String
)

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
    val printForm: String
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
    val vat: String,
    val discount: String,
    val storno: String,
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

data class QueueStrings(
    val title: String,
    val empty: String,
    val waiting: String,
    val attempts: String,
    val retryFailed: String,
    val retryDone: String
)

data class UserStrings(
    val title: String,
    val name: String,
    val create: String,
    val created: String,
    val forbiddenPin: String,
    val newPin: String,
    val change: String,
    val changed: String,
    val delete: String,
    val deleted: String,
    val role: String,
    val admin: String,
    val cashier: String
)

data class SettingStrings(
    val title: String,
    val currentKkm: String,
    val changeKkm: String,
    val registerKkm: String,
    val back: String,
    val registerKkmHint: String,
    val kkmIdentifier: String,
    val token: String,
    val adminPin: String,
    val registering: String,
    val register: String,
    val registered: String,
    val registerHint: String,
    val diagnostics: String,
    val ofdLink: String,
    val checkOfdLink: String,
    val ofdAnswers: String,
    val ofdSilent: String,
    val ofdInfo: String,
    val programmingMode: String,
    val enterProgramming: String,
    val exitProgramming: String,
    val enteredProgramming: String,
    val exitedProgramming: String,
    val ofd: String,
    val environment: String,
    val language: String,
    val appearance: String,
    val ofdToken: String,
    val newToken: String,
    val saveToken: String,
    val tokenSaved: String,
    val tokenHint: String,
    val printForm: String,
    val programmingRequired: String,
    val printFormSaved: String,
    val receiptLanguage: String,
    val receiptBoth: String,
    val receiptKk: String,
    val receiptRu: String,
    val paperWidth: String,
    val printLayout: String,
    val printer: String,
    val printerHint: String,
    val printerSystem: String,
    val printKind: String,
    val printCopies: String,
    val panelBehaviour: String,
    val groupAppearance: String,
    val groupService: String,
    val groupIrreversible: String,
    val panelBehaviourHint: String,
    val panelPositionEntry: String,
    val panelReceiptDetails: String,
    val panelMoney: String,
    val printLayoutHint: String,
    val layoutTape58: String,
    val layoutTape80: String,
    val layoutFullscreen: String,
    val printOfdAds: String,
    val printOfdAdsHint: String,
    val receiptLines: String,
    val receiptLinesHint: String,
    val saveReceiptLines: String,
    val lineBeforeHeader: String,
    val lineHeader: String,
    val lineAfterHeader: String,
    val lineBeforeItems: String,
    val lineAfterItems: String,
    val lineBeforeTotals: String,
    val lineAfterTotals: String,
    val lineBeforeQr: String,
    val lineFooter: String,
    val appearanceSystem: String,
    val appearanceLight: String,
    val appearanceDark: String,
    val factoryStep: String,
    val generateFactory: String,
    val factoryNumber: String,
    val manufactureYear: String,
    val factoryHint: String,
    val ofdStep: String,
    val localName: String,
    val localNameHint: String,
    val save: String
)

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
    val fit: String
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
