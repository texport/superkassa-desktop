package kz.mybrain.superkassa.strings.api.cabinet.documents

/** Документы кассы, которые БФД приняла: чеки, смены, отчёты и движение денег. */
data class DocumentTexts(
    val title: String,
    val receipts: String,
    val shifts: String,
    val reports: String,
    val cashMovements: String,
    val empty: String,

    /**
     * За выбранный срок документов нет, а у кассы они есть.
     *
     * Отдельно от «документов нет вовсе»: над списком стоят счётчики
     * за всё время, и «Здесь появится то, что БФД приняла от этой кассы»
     * рядом с сотней чеков читалось как потеря документов.
     */
    val noneInPeriod: String,
    val periodToday: String,
    val periodWeek: String,
    val periodMonth: String,
    val periodAll: String,
    val allShown: String,
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
    val deliveryRefused: String,
    val kgdMarked: String,
    val noKgdMark: String,

    /** Строка карточки о доставке документа в КГД. */
    val kgdDelivery: String,

    /** Почему КГД отклонил документ — подпись причины в карточке. */
    val kgdReason: String,

    /** Состояние в КГД: принят. */
    val kgdAccepted: String,

    /** Состояние в КГД: отклонён. */
    val kgdRejected: String,

    /** Состояние в КГД: не доставлен, попытки исчерпаны. */
    val kgdFailed: String,

    /** Состояние в КГД: отправлен, ответа ещё нет. */
    val kgdSent: String,

    /** Состояние в КГД: передаётся службе передачи. */
    val kgdTransferring: String,

    /** Состояние в КГД: ошибка передачи, будет повтор. */
    val kgdTransferFailed: String,

    /** Состояние в КГД: ждёт отправки. */
    val kgdAwaiting: String,

    /** X-отчёт и движение денег: в КГД не передаются. */
    val kgdNotSent: String,
    val moment: String,

    /**
     * Номер документа у БФД.
     *
     * Не регистрационный номер кассы: тот выдаёт КГД, он один на кассу
     * и стоит в паспорте. Здесь счётчик документа, и под чужой подписью
     * он читался как РНМ.
     */
    val number: String,
    val kkmDocumentNumber: String,

    /** Кабинет не отдал пакет протокола: рисовать документ не по чему. */
    val dataMissing: String,
    val receiptMoment: String,
    val receiptItems: String,
    val receiptPayments: String,
    val receiptTaxes: String,
    val receiptTotal: String,
    val receiptTaken: String,
    val receiptChange: String,
    val receiptDiscount: String,
    val receiptMarkup: String,
    val operator: String,
    val paymentCash: String,
    val paymentCard: String,
    val paymentElectronic: String,
    val paymentMobile: String,
    val paymentCredit: String,
    val paymentTare: String,
    val taxVat: String,
    val openedAt: String,
    val closedAt: String,
    val revenue: String,
    val cashInDrawer: String,
    val sales: String,
    val returns: String,
    val autonomous: String
)
