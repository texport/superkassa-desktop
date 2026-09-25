package kz.mybrain.superkassa.strings.api.common

import kz.mybrain.superkassa.strings.api.settings.LookTexts

/*
 * Надписи служебных разделов.
 *
 * Очередь, кассиры, настройки, показ печатной формы и словари состояний —
 * то, что открывают редко и осознанно.
 */

/** Надписи очереди неотправленных документов. */
data class QueueTexts(
    val title: String,
    val empty: String,
    val waiting: String,
    val attempts: String,
    val retryFailed: String,
    val retryDone: String
)

/** Надписи раздела кассиров. */
data class UserTexts(
    val title: String,
    val name: String,
    val create: String,
    val created: String,
    val newPin: String,
    val change: String,
    val changed: String,
    val delete: String,
    val deleted: String,
    val role: String,
    val admin: String,
    val cashier: String
)

/** Надписи экрана настроек: касса, рабочее место, печать, службы. */
data class SettingsScreenTexts(
    val title: String,
    val currentKkm: String,
    /**
     * У кассы нет сведений об организации.
     *
     * Надпись живёт здесь, а не в модели кассы: подставленная в модель
     * строка шла на казахский и английский экран по-русски и попадала
     * в то, по чему ищет список касс.
     */
    val orgUnknown: String,
    val changeKkm: String,
    val registerKkm: String,
    val back: String,
    val kkmIdentifier: String,
    val token: String,
    val adminPin: String,
    val registering: String,
    val register: String,
    val registered: String,
    val diagnostics: String,
    val ofdLink: String,
    val checkOfdLink: String,
    val ofdAnswers: String,
    val ofdSilent: String,
    val ofdInfo: String,
    val programmingMode: String,
    val enterProgramming: String,
    val exitProgramming: String,
    /** Плашка состояния: касса сейчас в режиме программирования. */
    val programmingOn: String,
    val enteredProgramming: String,
    val exitedProgramming: String,
    val ofd: String,
    val environment: String,
    val language: String,
    val appearance: String,
    val appearanceHint: String,
    val look: LookTexts,
    val ofdToken: String,
    val newToken: String,
    val saveToken: String,
    val tokenSaved: String,
    val tokenHint: String,
    val printForm: String,
    val printFormHint: String,
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
    /** На машине нет ни одного принтера: печатать чек некуда. */
    val printerNone: String,
    val printKind: String,
    val printCopies: String,
    val panelBehaviour: String,
    val taxSettings: String,
    val taxSettingsHint: String,
    val taxRegime: String,
    /**
     * Отрасль кассы: от неё зависит, что кассир заполняет в каждом чеке.
     *
     * Хранится на этом рабочем месте, а не на узле: узлу вид отрасли
     * приходит с каждым чеком, и настройки под него у него нет.
     */
    val tradeDomain: String,
    val tradeDomainHint: String,
    val domainKind: String,
    val domainFields: String,
    val defaultVatGroup: String,
    /** Справочники узла не прочитаны: выбирать не из чего и незачем. */
    val dictionariesMissing: String,
    val dictionariesMissingHint: String,
    val autoCashout: String,
    val autoCashoutHint: String,
    val settingsSaved: String,
    val addressMalformed: String,
    val mapServices: String,
    val mapServicesHint: String,
    val mapTiles: String,
    val mapSearch: String,
    val mapReverse: String,
    val mapLocation: String,
    val mapDefault: String,
    val panelBehaviourHint: String,
    val panelPositionEntry: String,
    val panelReceiptChanges: String,
    val panelCustomerData: String,
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
    val ofdStep: String,
    val localName: String,
    val localNameHint: String,
    val save: String
)
