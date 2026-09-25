package kz.mybrain.superkassa.strings.impl.common

import kz.mybrain.superkassa.strings.api.common.QueueTexts
import kz.mybrain.superkassa.strings.api.common.SettingsScreenTexts
import kz.mybrain.superkassa.strings.api.common.UserTexts
import kz.mybrain.superkassa.strings.impl.settings.lookTextsRu

/** Надписи [QueueTexts] по-русски. */
internal val queueTextsRu = QueueTexts(
    title = "Очередь отправки",
    empty = "Ждущих документов нет — всё доставлено в БФД.",
    waiting = "Ждут отправки",
    attempts = "Попыток",
    retryFailed = "Повторить неудачные",
    retryDone = "Неудачные задачи поставлены на повтор"
)

/** Надписи [UserTexts] по-русски. */
internal val userTextsRu = UserTexts(
    title = "Кассиры и пины",
    name = "Имя",
    create = "Завести",
    created = "заведён",
    newPin = "Новый пин",
    change = "Сменить",
    changed = "Пин изменён",
    delete = "Удалить",
    deleted = "удалён",
    role = "Роль",
    admin = "Администратор",
    cashier = "Кассир"
)

/** Надписи [SettingsScreenTexts] по-русски. */
internal val settingsScreenTextsRu = SettingsScreenTexts(
    title = "Настройки",
    currentKkm = "Касса в работе",
    orgUnknown = "Организация не указана",
    changeKkm = "Сменить кассу",
    registerKkm = "Завести кассу",
    back = "Назад",
    kkmIdentifier = "Идентификатор ККМ",
    token = "Токен",
    adminPin = "Пин администратора",
    registering = "Заводится…",
    register = "Завести",
    registered = "Касса заведена, состояние",
    diagnostics = "Диагностика",
    ofdLink = "Связь с БФД",
    checkOfdLink = "Проверить связь с БФД",
    ofdAnswers = "БФД отвечает",
    ofdSilent = "БФД не отвечает",
    ofdInfo = "Сведения о БФД",
    programmingMode = "Режим программирования",
    enterProgramming = "Войти в программирование",
    exitProgramming = "Выйти из программирования",
    programmingOn = "Включён",
    enteredProgramming = "Касса переведена в режим программирования",
    exitedProgramming = "Касса выведена из режима программирования",
    ofd = "БФД",
    environment = "Контур",
    language = "Язык",
    appearance = "Оформление",
    appearanceHint = "Вид этого рабочего места. Хранится на этой машине: на чеках, на других кассах и в " +
        "кабинете ничего не меняется",
    look = lookTextsRu,
    ofdToken = "Токен БФД",
    newToken = "Новый токен",
    saveToken = "Сохранить токен",
    tokenSaved = "Токен сохранён",
    tokenHint = "Токен выдаёт БФД. По ответу «неверный токен» касса встаёт " +
        "и работает снова только после ввода нового.",
    printForm = "Печатный чек",
    printFormHint = "Как выглядит печатный чек: язык, ширина ленты и что именно печатать. Форма обязана сходиться с " +
        "тем, что уехало в БФД, — это один и тот же документ",
    programmingRequired = "Настройки кассы меняются в режиме программирования.",
    printFormSaved = "Настройки чека сохранены",
    receiptLanguage = "Язык чека",
    receiptBoth = "Два языка",
    receiptKk = "Қазақша",
    receiptRu = "Русский",
    paperWidth = "Ширина ленты",
    printLayout = "Макет печати",
    printer = "Принтер кассы",
    printerHint = "Принтер держится за кассой: за одним компьютером их бывает две, " +
        "и чековая лента у каждой своя. Пока не выбран, задание уходит на системный по умолчанию.",
    printerSystem = "Системный по умолчанию",
    printerNone = "На этой машине нет ни одного принтера: чек напечатать некуда. " +
        "Подключите принтер и откройте настройки заново",
    printKind = "Вид файла при сохранении",
    printCopies = "Копий при печати",
    panelBehaviour = "Разделы кассовой колонки",
    taxSettings = "Налоги кассы",
    taxSettingsHint = "Режим налогообложения и ставка НДС этой кассы: из них считается налог в каждом чеке. Задаются " +
        "по данным КГД — при расхождении чеки уйдут с неверным налогом",
    taxRegime = "Налоговый режим",
    tradeDomain = "Отрасль кассы",
    tradeDomainHint = "Протокол требует вид отрасли у каждого чека, а отрасль у кассы одна: выбранная здесь " +
        "решает, какие реквизиты кассир заполняет на продаже. У торговли их нет",
    domainKind = "Вид отрасли",
    domainFields = "Кассир заполняет на продаже",
    defaultVatGroup = "Ставка по умолчанию",
    dictionariesMissing = "Справочники кассы не прочитаны",
    dictionariesMissingHint = "Режимы налогообложения и ставки НДС приходят из кассы. " +
        "Касса не ответила, когда рабочее место их спрашивало — попробуйте ещё раз.",
    autoCashout = "Изымать наличные при закрытии смены",
    autoCashoutHint = "Касса сама оформит изъятие остатка наличных вместе с Z-отчётом. Без него деньги из ящика " +
        "перейдут в новую смену, и остаток в кассе разойдётся с тем, что в ящике лежит.",
    settingsSaved = "Настройки кассы сохранены",
    addressMalformed = "Адрес начинается с http:// или https:// и не содержит пробелов",
    mapServices = "Службы карты",
    mapServicesHint = "Пока адрес не задан, работает общедоступная карта сообщества: на всех владельцев она не " +
        "рассчитана",
    mapTiles = "Плитки карты",
    mapSearch = "Поиск адреса",
    mapReverse = "Адрес по метке",
    mapLocation = "Определение места",
    mapDefault = "Вернуть общедоступные",
    panelBehaviourHint = "Выбранное здесь — то, что кассир увидит при открытии продажи. " +
        "Свернуть или развернуть раздел он всё равно может стрелкой на самом экране.",
    panelPositionEntry = "Новая позиция",
    panelReceiptChanges = "Скидки и наценки",
    panelCustomerData = "Данные покупателя",
    panelMoney = "Оплата и итог",
    panelTill = "Кассовая колонка",
    printLayoutHint = "Лента 58 и 80 мм — для чековых принтеров, страница — для печати на обычном листе " +
        "и для отправки покупателю.",
    layoutTape58 = "Лента 58 мм",
    layoutTape80 = "Лента 80 мм",
    layoutFullscreen = "Полная страница",
    printOfdAds = "Печатать рекламу БФД",
    printOfdAdsHint = "Строки приходят вместе с ответом на чек и печатаются под итогом.",
    receiptLines = "Свои строки на чеке",
    receiptLinesHint = "Реклама оператора приходит от БФД, а это строки самой торговой точки: " +
        "приветствие, условия возврата, благодарность. Пустое поле не печатается.",
    saveReceiptLines = "Сохранить строки",
    lineBeforeHeader = "Над шапкой",
    lineHeader = "В шапке",
    lineAfterHeader = "Под шапкой",
    lineBeforeItems = "Перед позициями",
    lineAfterItems = "После позиций",
    lineBeforeTotals = "Перед итогом",
    lineAfterTotals = "После итога",
    lineBeforeQr = "Перед QR-кодом",
    lineFooter = "В подвале",
    appearanceSystem = "Как в системе",
    appearanceLight = "Светлая",
    appearanceDark = "Тёмная",
    factoryStep = "Шаг 1. Заводские данные",
    generateFactory = "Сгенерировать",
    factoryNumber = "Заводской номер",
    manufactureYear = "Год выпуска",
    ofdStep = "Шаг 2. Данные от БФД",
    localName = "Название кассы",
    localNameHint = "Название хранится в кассе и видно на экране входа. " +
        "Прочие сведения приходят от БФД и здесь не меняются.",
    save = "Сохранить"
)
