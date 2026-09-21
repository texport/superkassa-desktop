package kz.mybrain.superkassa.desktop.ui.strings

/**
 * Надписи области «Личный кабинет БФД».
 *
 * Кабинет — рабочее место владельца, а не кассира: здесь заводят кассы,
 * подают заявления в ИСНА и смотрят, что доехало до БФД. Слова взяты
 * из речи владельца и из формулировок КГД, а не из имён полей контракта.
 *
 * Значения вынесены по языкам в соседние файлы — так же, как у надписей
 * кассы: три набора по полторы сотни строк в одном файле не читаются,
 * а расхождение между языками в них не разглядеть.
 */
data class CabinetTexts(
    val title: String,
    val signIn: String,
    val signing: String,
    val signOut: String,
    val noNcaLayer: String,
    val signDeclined: String,
    val signWindowClosed: String,
    val sessionExpired: String,
    val unreachable: String,
    val address: String,
    val company: String,
    val bin: String,
    val okeds: String,
    val okedsEmpty: String,
    val primaryOked: String,
    val saveOkeds: String,
    val places: String,
    val placesEmpty: String,
    val addPlace: String,
    val placeName: String,
    val placeAddress: String,
    val findAddress: String,
    val registerCount: String,
    val registers: String,
    val registersEmpty: String,
    val addRegister: String,
    val factoryNumber: String,
    val manufactureYear: String,
    val model: String,
    val modelNotFound: String,
    val internalName: String,
    val status: String,
    val registrationNumber: String,
    val technicalState: String,
    val shift: String,
    val lastContact: String,
    val issueToken: String,
    val tokenIssued: String,
    val registration: String,
    val reregistration: String,
    val deregistration: String,
    val actionsEmpty: String,
    val reason: String,
    val comment: String,
    val newPlace: String,
    val card: String,
    val cardMissing: String,
    val savePdf: String,
    /** Версии регистрационной карты: прежние записи КГД о кассе. */
    val cardVersions: String,
    val cardVersionsEmpty: String,
    val cardVersion: String,
    val cardCurrentVersion: String,
    val cardChanged: String,
    val documents: String,
    val receipts: String,
    val shifts: String,
    val reports: String,
    val cashMovements: String,
    val documentsEmpty: String,
    val chooseRegister: String,
    val refresh: String,
    val add: String,
    val remove: String,
    val save: String,
    val reasonCessation: String,
    val reasonBroken: String,
    val reasonLost: String,
    val reasonOther: String,
    val iin: String,
    val place: String,
    val makePrimary: String,
    val latitude: String,
    val longitude: String,
    val rename: String,
    val changeAddress: String,
    val deleteRegister: String,
    val deleteOnlyDraft: String,
    val statusDraft: String,
    val statusRegistered: String,
    val statusDeregistered: String,
    val statusActive: String,
    val statusInactive: String,
    val statusAccepted: String,
    val statusRejected: String,
    val statusSent: String,
    val statusInProcess: String,
    val shiftOpen: String,
    val shiftClosed: String,
    val operationSale: String,
    val operationReturn: String,
    val operationPurchase: String,
    val operationPurchaseReturn: String,
    val reportZ: String,
    val reportX: String,
    val deposit: String,
    val withdrawal: String,
    val delivered: String,
    val pending: String,
    val kgdMarked: String,
    val receiptItems: String,
    val receiptPayments: String,
    val receiptTaxes: String,
    val receiptTotal: String,
    val operator: String,
    val close: String,
    val companyFromEds: String,
    val pickRegisterFirst: String,
    val technicalUnknown: String,
    val trafficSuspended: String,
    val bfdDisconnected: String,
    val passport: String,
    val applications: String,
    val actionsJournal: String,
    val token: String,
    val submitApplication: String,
    val applicationSent: String,
    val registerStatus: String,
    val applicationWait: String,
    val required: String,
    val optional: String,
    val foundAddresses: String,
    val addressChosen: String,
    val addOked: String,
    val placeRemoveBlocked: String,
    val receiptMoment: String,
    val kkmDocumentNumber: String,
    val noKgdMark: String,
    /** Кабинет не отдал пакет протокола: рисовать документ не по чему. */
    val documentDataMissing: String,
    val receiptTaken: String,
    val receiptChange: String,
    val receiptDiscount: String,
    val receiptMarkup: String,
    val paymentCash: String,
    val paymentCard: String,
    val paymentElectronic: String,
    val paymentMobile: String,
    val paymentCredit: String,
    val paymentTare: String,
    val taxVat: String,
    val deliveryRefused: String,
    val factoryLocked: String,
    val factoryIssued: String,
    val tokenOnlyRegistered: String,
    val kkmNotActive: String,
    val defaultPinNotAllowed: String,
    val missing: String,
    val periodToday: String,
    val periodWeek: String,
    val periodMonth: String,
    val periodAll: String,
    val showMore: String,
    val allShown: String,
    val shownOf: String,
    val okedSearch: String,
    val okedNotFound: String,

    /** Кабинет отказал снять кассу: смена не закрыта. */
    val shiftOpenTitle: String,
    val shiftOpenAsk: String,
    val shiftOpenElsewhere: String,
    val closeShiftAndDeregister: String,
    val adminPin: String,

    /** Сверка состояний: кто о кассе говорит и что именно. */
    val stateDisagree: String,
    val stateDisagreeNote: String,
    val sourceNode: String,
    val sourceCabinet: String,
    val sourceBfd: String,

    /** Ответ по существу: то, что владелец читает первой строкой. */
    val stateWorking: String,
    val stateBlocked: String,
    val stateOffRecord: String,
    val stateWorkUnknown: String,
    val stateShiftUnknown: String,

    /**
     * Показание одного источника.
     *
     * Плашка читается отдельно от всего: не «Нет», а «касса снята
     * с учёта» — под общим «Нет» владелец не понимал ни того, о чём речь,
     * ни того, что ему делать.
     */
    val claimWorking: String,
    val claimBlocked: String,
    val claimOnRecord: String,
    val claimOffRecord: String,
    val claimRecordUnread: String,
    val claimNodeNoKkm: String,
    val claimBfdNoKkm: String,
    val claimBfdNoAnswer: String,
    val claimShiftOpen: String,
    val claimShiftClosed: String,
    val claimShiftNotKept: String,

    /** Снимок БФД: номер смены, связь и отсутствие того и другого. */
    val shiftNumberTitle: String,
    val shiftNumberNone: String,
    val lastContactNever: String,

    /** Выдача классификатора упёрлась в предел: дальше списка нет. */
    val okedNarrowSearch: String,
    val pickOnMap: String,
    val pointNotChosen: String,
    val noApplications: String,
    val applicationInFlight: String,
    val openedAt: String,
    val closedAt: String,
    val revenue: String,
    val cashInDrawer: String,
    val sales: String,
    val returns: String,
    val autonomous: String,
    val documentMoment: String,

    /**
     * Номер документа у БФД.
     *
     * Не регистрационный номер кассы: тот выдаёт КГД, он один на кассу
     * и стоит в паспорте. Здесь счётчик документа, и под чужой подписью
     * он читался как РНМ.
     */
    val documentNumber: String,
    val developerSignIn: String,
    val addressRegion: String,
    val addressLocality: String,
    val addressStreet: String,
    val addressBuilding: String,
    val okedCode: String,
    val okedName: String,
    val stagePreparing: String,
    val stageSigning: String,
    val stageSending: String,
    val applicationFailed: String,
    val addressPickAgain: String,
    val addressNotFound: String,
    /** Поиск по колонке торговых точек: их бывают сотни. */
    val placeSearch: String,
    val placeNotFound: String,
    /**
     * Подсказки разделов кабинета: что это за раздел и зачем он владельцу.
     *
     * Объяснение предмета, а не подпись к заголовку: «Виды деятельности»
     * без него читались как список неизвестно чего. Собраны одним блоком,
     * потому что все выходят под значком у заголовка карточки и правятся
     * вместе — разойдясь по набору, они начали расходиться и по языку.
     */

    /** Надписи выбора точки на карте. */
    val map: MapTexts,
    val hints: CabinetHints
)

/**
 * Объяснения разделов кабинета — те, что живут под значком у заголовка.
 *
 * Отдельной группой, а не в общем наборе: их три десятка, и вместе
 * с остальными надписями набор кабинета перевалил за предел JVM
 * в 255 аргументов конструктора — класс перестал загружаться вовсе,
 * и это не поймала ни сборка, ни один тест: `ClassFormatError` случается
 * при загрузке. Группа заодно отвечает на вопрос «где подсказка этого
 * раздела»: искать её в одном месте, а не среди двух с половиной сотен
 * полей.
 */
data class CabinetHints(
    val signIn: String,

    /**
     * Что идёт, пока владелец ждёт подписи, и что делать, если окна
     * NCALayer на экране нет.
     *
     * Окно подписи открывает не приложение, а NCALayer, и встать оно
     * может за главным окном: владелец смотрел на ожидание, считая,
     * что подписывать ещё нечего.
     */
    val signWait: String,

    /**
     * Запрос NCALayer принял, а подписи не вернул.
     *
     * Своё объяснение, а не «Запустите NCALayer»: он запущен и на связи.
     * Ровно эту строку про работающий NCALayer владелец и читал спустя
     * три минуты ожидания.
     */
    val signNoAnswer: String,
    val address: String,
    val cardVersionsEmpty: String,
    val placesEmpty: String,
    val registersEmpty: String,
    val okedsEmpty: String,
    val chooseRegister: String,
    val documentsEmpty: String,
    val actionsEmpty: String,
    val pickRegisterFirst: String,
    val technicalUnknown: String,
    val token: String,
    val internalName: String,
    val receipts: String,
    val cardMissing: String,
    val okedSearch: String,
    val technicalState: String,
    val bfdNoAnswer: String,
    val developerSignIn: String,
    val addressStep: String,
    val okedManual: String,
    val placeNotFound: String,
    val registers: String,
    val places: String,
    val placesTree: String,
    val okeds: String,
    val passport: String,
    val onThisMachine: String,
    val receiptCard: String,
    val reportCard: String,
    val shiftCard: String,
    val cashMovement: String,
    val factoryNumber: String
)

/** Надписи области на выбранном языке. */
fun cabinetTexts(language: Language): CabinetTexts = when (language) {
    Language.Kk -> cabinetTextsKk
    Language.Ru -> cabinetTextsRu
    Language.En -> cabinetTextsEn
}
