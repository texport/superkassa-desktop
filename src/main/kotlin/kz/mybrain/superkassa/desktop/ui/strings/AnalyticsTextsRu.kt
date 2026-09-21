package kz.mybrain.superkassa.desktop.ui.strings

/** Надписи торговой сводки по-русски. Состав полей задан в [AnalyticsSalesTexts]. */
private val analyticsSalesTextsRu = AnalyticsSalesTexts(
    tab = "Торговля",
    forPeriod = "Сводка за",

    revenue = "Выручка",
    receipts = "Чеков",
    average = "Средний чек",
    refunds = "Возвраты",
    tax = "Налог",
    net = "Чистыми",

    empty = "За этот срок документов нет",
    emptyHint = "Возьмите срок шире — неделю или месяц — или перелистните его стрелками",

    openShifts = "Открытых смен",
    offline = "Автономных",
    queuedCount = "Ждут ответа",
    unknownCount = "Без сведений",

    paidOut = "Выплачено",
    purchasesHint = "Скупленное у населения касса оплачивает из ящика: эти деньги не выручка, и в числа выше они не входят",

    byDay = "Выручка по дням",
    byHour = "Нагрузка по часам",
    payments = "Виды расчётов",
    paymentOther = "Прочее",
    noPayments = "Расчётов за этот срок не было",
    nothingToDraw = "Рисовать за этот срок нечего",

    registers = "Кассы",
    places = "Торговые точки",
    allRegistersShown = "Показаны все кассы",
    allPlacesShown = "Показаны все точки",
    colName = "Название",

    delivery = "Доставка документов",
    delivered = "Доставлено",
    queued = "В очереди",
    unknown = "Без сведений",
    rejected = "Отбраковано"
)

/** Надписи аналитики по-русски. Состав полей задан в [AnalyticsTexts]. */
internal val analyticsTextsRu = AnalyticsTexts(
    title = "Аналитика",
    mapTab = "Кассы на карте",
    exchangeTab = "Адреса обмена",

    positionSource = "Положение брать",
    sourceAddress = "По адресу точки",
    sourceCabinet = "По кабинету",
    sourceKkm = "По кассе",
    sourceAddressHint = "Где касса числится по учёту КГД: адрес торговой точки. " +
        "Координат у адресного регистра нет — дом ищет карта",
    sourceCabinetHint = "Где владелец поставил точку сам, когда заводил торговую точку",
    sourceKkmHint = "Где касса считает себя находящейся: координаты прислала сама машина",

    refresh = "Обновить",
    unreachable = "Кабинет не отвечает",
    unreachableHint = "Проверьте адрес кабинета в настройках и связь с ним, затем обновите",
    notDeployed = "Аналитики в кабинете ещё нет",
    notDeployedHint = "Кабинет ответил, что такого раздела не знает: он ещё не выложен. " +
        "Экран заработает сам, как только выкладка пройдёт",
    refused = "Кабинет отказал",

    placed = "На карте",
    withoutPosition = "Без положения",
    kkmCount = "Касс",
    kkmColumn = "Касса",
    addressCount = "Адресов",

    mapEmpty = "Ни одной кассы на карте",
    mapEmptyHint = "При этом источнике положения поставить на карту нечего — посмотрите список рядом",
    pickPin = "Выберите кассу на карте",
    pickPinHint = "Нажмите точку — здесь появится её карточка",

    searchKkm = "Название, номер КГД или адрес",
    allPlaces = "Все торговые точки",
    markShiftOpen = "Смена открыта",
    markBlocked = "Заблокированные",
    markOffRecord = "Не на учёте",
    sieveEmpty = "Под отбор не подошла ни одна касса",
    sieveEmptyHint = "Снимите часть плашек отбора или измените строку поиска",
    sieveClear = "Сбросить отбор",
    kkmsHere = "Кассы в этом месте",
    openKkmSales = "Аналитика кассы",
    kkmSalesTitle = "Чем торгует касса",

    withoutPositionEmpty = "Все кассы на карте",
    withoutPositionEmptyHint = "При этом источнике положения известно место каждой кассы",
    reasonNoAddress = "У торговой точки не выбран адрес",
    reasonNoCabinetPoint = "В кабинете не заданы координаты торговой точки",
    reasonNoKkmPoint = "Касса ни разу не присылала свои координаты",
    reasonSearching = "Ищем адрес на карте",
    reasonNotOnMap = "Карта не нашла этот адрес",

    registrationNumber = "Номер КГД",
    noRegistrationNumber = "Номер КГД не присвоен",
    retailPlace = "Торговая точка",
    blocked = "Заблокирована",
    lastContact = "Последняя связь",
    neverSeen = "Связи не было",
    positionFrom = "Положение",
    fromAddress = "По адресу торговой точки",
    fromCabinet = "По координатам кабинета",
    fromKkm = "По координатам кассы",
    geoSource = "Способ определения",

    exchangeTitle = "Адреса, с которых кассы присылали данные",
    exchangeHint = "Сведение служебное: по нему видно, что касса выходит на связь не оттуда, " +
        "где числится, и что под одним адресом работает несколько машин",
    exchangeEmpty = "Обменов ещё не было",
    exchangeEmptyHint = "Адрес появится здесь после первой связи кассы с сервисом приёма",
    exchangeNotFound = "Ничего не нашлось",
    exchangeNotFoundHint = "Измените строку поиска или снимите отбор по кассе",
    search = "Поиск по адресу или кассе",
    allRegisters = "Все кассы",
    exchangeAddress = "Адрес",
    firstSeen = "Впервые",
    lastSeen = "В последний раз",

    sales = analyticsSalesTextsRu
)
