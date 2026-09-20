package kz.mybrain.superkassa.desktop.ui.strings

/**
 * Надписи области «Деньги, кассиры и настройки».
 *
 * Файл принадлежит одной области интерфейса целиком: так надпись заводится
 * в одном месте и переиспользуется, а правки разных экранов не сходятся
 * в одном файле. Каждое поле обязано существовать во всех трёх языках —
 * об этом заботится компилятор.
 *
 * Названия ролей, видов документов и состояний доставки сюда не попадают:
 * их отдаёт справочник узла сразу на трёх языках, и свой перевод рано или
 * поздно разошёлся бы с тем, что напечатано на чеке.
 */
data class MoneyTexts(
    val drawer: DrawerTexts,
    val cashiers: CashierTexts,
    val kkm: KkmSetupTexts
)

/** Денежный ящик: остаток, внесение, изъятие. */
data class DrawerTexts(
    val inDrawer: String,
    val countedByNode: String,
    val unknownBalance: String,
    val shiftClosed: String,
    /** Почему заблокированной кассе не проводят деньги. */
    val kkmBlocked: String,
    val shiftClosedShort: String,
    val amountHint: String,
    val notANumber: String,
    val notPositive: String,
    val tooLarge: String,
    val notEnough: String,
    val confirmDeposit: String,
    val confirmWithdraw: String,
    val afterOperation: String,
    val confirm: String,
    val cancel: String,
    val working: String,
    val recent: String,
    val recentEmpty: String,
    val recentEmptyHint: String
)

/** Кассиры кассы и их пины. */
data class CashierTexts(
    val addTitle: String,
    val listTitle: String,
    val pinLength: String,
    val pinUnique: String,
    val roles: String,
    val ownPin: String,
    val pinTooShort: String,
    val pinTooLong: String,
    val nameRequired: String,
    val deleteConfirm: String,
    val deleteExplain: String,
    val deleteBlocked: String,
    val changePinFor: String,
    val onlyInRole: String,
    val empty: String,
    val emptyHint: String
)

/** Заведение кассы, сверка с ОФД и снятие с учёта. */
data class KkmSetupTexts(
    val stepOne: String,
    val stepTwo: String,
    val copy: String,
    val copied: String,
    val factoryReady: String,
    val renameSaved: String,
    val renameReset: String,
    val diagnosticsHint: String,
    val diagnosticsEmpty: String,
    val programmingOn: String,
    val programmingOff: String,
    val ofdEmpty: String,
    val ofdAnswer: String,
    val ofdKgdNumber: String,
    val ofdFactoryNumber: String,
    val ofdSystemId: String,
    val ofdProtocol: String,
    val ofdOrganization: String,
    val ofdAddress: String,
    val ofdBin: String,
    val nodeTitle: String,
    val nodeMode: String,
    val nodeProtocol: String,
    val nodeTimeout: String,
    val nodeStorage: String,
    val syncTitle: String,
    val syncService: String,
    val syncServiceHint: String,
    val syncServiceDone: String,
    val syncCounters: String,
    val syncCountersHint: String,
    val syncCountersDone: String,
    val decommission: String,
    val decommissionHint: String,
    val needProgramming: String,
    val needShiftClosed: String,
    val needQueueEmpty: String,
    val needOnline: String,
    val decommissionConfirm: String,
    val decommissionDone: String
)

/** Надписи области на выбранном языке. */
fun moneyTexts(language: Language): MoneyTexts = when (language) {
    Language.Kk -> moneyTextsKk
    Language.Ru -> moneyTextsRu
    Language.En -> moneyTextsEn
}
