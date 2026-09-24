package kz.mybrain.superkassa.strings.impl.common

import kz.mybrain.superkassa.strings.api.common.AutonomousStrings
import kz.mybrain.superkassa.strings.api.common.CashStrings
import kz.mybrain.superkassa.strings.api.common.DashboardStrings
import kz.mybrain.superkassa.strings.api.common.ReturnStrings
import kz.mybrain.superkassa.strings.api.common.SaleStrings

/** Надписи [DashboardStrings] по-русски. */
internal val dashboardStringsRu = DashboardStrings(
    state = "Состояние",
    shift = "Смена",
    shiftOpenNo = "№ %s открыта",
    shiftClosed = "Закрыта",
    documentsInShift = "Документов за смену",
    shiftDocuments = "Документы смены",
    refused = "Отклонено БФД",
    refusedHint = "Эти документы БФД не приняла: фискальными они не стали, в счётчики смены не вошли " +
        "и печатной формы у них нет. Код отказа и кассир нужны обслуживанию.",
    openShift = "Открыть смену",
    xReport = "X-отчёт",
    closeShift = "Закрыть смену",
    closeShiftAsk = "Закрыть смену и снять Z-отчёт?",
    closeShiftExplain = "В смене документов: %s, в ящике %s. Z-отчёт уйдёт в БФД, и смена не отменяется.",
    closeShiftCashout = "Остаток наличных уйдёт изъятием вместе с отчётом.",
    closeShiftKeepsCash = "Остаток наличных останется в ящике и перейдёт в новую смену.",
    shiftTooLong = "Смена открыта дольше суток — закройте её Z-отчётом, иначе БФД заблокирует кассу.",
    shiftDayLimit = "Закройте смену до %s: после этого касса откажет в чеках, возвратах и деньгах.",
    openShiftHint = "Смена закрыта — откройте смену, чтобы пробивать чеки",
    openShiftAdmin = "Смену открывает администратор — позовите его, чтобы начать день.",
    shiftOpened = "Смена открыта",
    shiftClosedDone = "Смена закрыта, Z-отчёт отправлен",
    xReportDone = "X-отчёт сформирован",
    printForm = "Печатная форма",
    shiftUnknown = "Неизвестна",
    shiftEmpty = "Документов пока нет",
    shiftEmptyHint = "Смена открыта: первый чек появится здесь сразу после проведения.",
    documentsUnread = "Документы смены прочитать не удалось",
    documentsUnreadHint = "Смена открыта, а её документы касса не отдала: сколько их, неизвестно.",
    shiftUnknownHint = "Касса не назвала состояние смены — перечитайте кассу; " +
        "если не поможет, позовите обслуживание.",
    documentNo = "№"

)

/** Надписи [AutonomousStrings] по-русски. */
internal val autonomousStringsRu = AutonomousStrings(
    title = "Автономный режим",
    explain = "Связи с БФД нет. Чеки пробиваются и получают автономный признак, " +
        "а в БФД уходят сами, как только связь появится. Работать можно.",
    waiting = "Ждут отправки",
    checkLink = "Проверить связь",
    sendQueued = "Отправить накопленное",
    sendQueuedRules = "Досылку накопленного делает администратор: смена должна быть закрыта, а касса переведена в режим программирования.",
    linkBack = "Связь с БФД восстановлена"
)

/** Надписи [SaleStrings] по-русски. */
internal val saleStringsRu = SaleStrings(
    receipt = "Чек",
    sale = "Продажа",
    purchase = "Покупка",
    issueSale = "Пробить чек",
    issuePurchase = "Оформить покупку",
    issuing = "Оформляется…",
    name = "Наименование",
    price = "Цена",
    quantity = "Количество",
    measureUnit = "Единица измерения",
    vat = "НДС",
    discount = "Скидка",
    storno = "Сторно",
    stornoUndo = "Снять сторно",
    add = "Добавить",
    remove = "Убрать",
    clearBasket = "Очистить корзину",
    receiptDiscount = "Скидка на чек",
    receiptMarkup = "Наценка на чек",
    discountOrMarkup = "Скидка и наценка на чек — одна или другая",
    customerBin = "ИИН/БИН покупателя",
    taken = "Принято",
    total = "Итого",
    barcode = "Штрихкод",
    barcodeSearch = "Поиск по штрихкоду",
    barcodeSearching = "Ищется…",
    barcodeFind = "Найти",
    barcodeMissing = "В справочнике нет такого штрихкода — добавьте позицию вручную.",
    barcodeUnavailable = "Справочник сейчас недоступен — добавьте позицию вручную.",
    payment = "Вид оплаты",
    delivered = "чек пробит и доставлен в БФД",
    queued = "чек пробит, связи нет — поставлен в очередь"
)

/** Надписи [ReturnStrings] по-русски. */
internal val returnStringsRu = ReturnStrings(
    title = "Возврат",
    empty = "В текущей смене нет чеков, по которым можно оформить возврат.",
    receiptNo = "Чек №",
    refundFor = "Возврат по чеку №",
    saleReturn = "Возврат продажи",
    giveBack = "Вернуть покупателю",
    purchaseReturn = "Возврат покупки",
    takeBack = "Принять обратно",
    done = "оформлен, состояние"
)

/** Надписи [CashStrings] по-русски. */
internal val cashStringsRu = CashStrings(
    deposit = "Внести",
    withdraw = "Изъять",
    deposited = "Внесено",
    withdrawn = "Изъято"
)
