package kz.mybrain.superkassa.strings.impl.common

import kz.mybrain.superkassa.strings.api.common.EnumStrings
import kz.mybrain.superkassa.strings.api.common.PreviewStrings
import kz.mybrain.superkassa.strings.api.common.StatusStrings

/** Надписи [PreviewStrings] по-русски. */
internal val previewStringsRu = PreviewStrings(
    title = "Печатная форма",
    narrower = "Уже",
    wider = "Шире",
    close = "Закрыть",
    missing = "Касса не нарисовала печатную форму",
    print = "Печать",
    printSent = "Отправлено на принтер",
    printFailed = "Принтер не принял задание",
    printerMissing = "На этой машине нет ни одного принтера: печатать некуда",
    save = "Сохранить в файл",
    saved = "Сохранено",
    zoomIn = "Крупнее",
    zoomOut = "Мельче",
    fit = "По ширине",
    drawPin = "Пин кассы",
    drawPinHint = "Печатную форму рисует касса и пускает к ней по пину. " +
        "Пин остаётся только в памяти приложения и разделы кассы не открывает.",
    draw = "Показать форму",
    noDrawer = "Документ пришёл из кабинета, а печатную форму по нему рисует касса " +
        "на этой машине. Здесь не заведено ни одной кассы — рисовать нечем."
)

/** Надписи [StatusStrings] по-русски. */
internal val statusStringsRu = StatusStrings(
    delivered = "Доставлен",
    resent = "Досланный",
    refused = "Отклонён",
    internal = "Внутренний",
    queued = "В очереди",
    unknown = "Состояние неизвестно"
)

/** Надписи [EnumStrings] по-русски. */
internal val enumStringsRu = EnumStrings(
    vatNone = "Без НДС",
    vat0 = "НДС 0%",
    vat5 = "НДС 5%",
    vat10 = "НДС 10%",
    vat16 = "НДС 16%",
    paymentCash = "Наличные",
    paymentCard = "Карта",
    paymentElectronic = "Электронные",
    paymentMobile = "Мобильный платёж",
    paymentCredit = "В кредит",
    paymentTare = "Тарой",
    domainTrading = "Торговля",
    domainServices = "Услуги",
    domainHotels = "Гостиницы",
    domainGasOil = "Нефтепродукты",
    domainTaxi = "Такси",
    domainParking = "Стоянка",
    docCheck = "Чек",
    docSale = "Продажа",
    docReturn = "Возврат продажи",
    docBuy = "Покупка",
    docBuyReturn = "Возврат покупки",
    docShiftOpen = "Открытие смены",
    docShiftClose = "Закрытие смены",
    docCashIn = "Внесение",
    docCashOut = "Изъятие",
    docReportX = "X-отчёт",
    stateActive = "В работе",
    stateIdle = "Готова к работе",
    stateBlocked = "Заблокирована",
    stateProgramming = "Программирование",
    stateRegistration = "Регистрация"
)
