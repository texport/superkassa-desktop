package kz.mybrain.superkassa.strings.impl.cabinet.eds

import kz.mybrain.superkassa.strings.api.cabinet.eds.EdsTexts

/** Надписи [EdsTexts] по-русски. */
internal val edsTextsRu = EdsTexts(
    remaining = "Осталось %s",
    cancelWait = "Отменить ожидание",
    ncaLayer = "NCALayer",
    egov = "eGov mobile",
    keyFile = "Файл ключа",
    aboutEgov = "Подпись в приложении eGov mobile: на этом устройстве или по QR с телефона. Ключ остаётся в eGov " +
        "mobile",
    aboutKeyFile = "Подпись файлом ключа .p12 на этом устройстве. Пароль вводится при каждой подписи и нигде не " +
        "сохраняется",
    waitEgov = "Подпишите в eGov mobile: отсканируйте QR телефоном или откройте приложение на этом устройстве",
    waitKeyFile = "Выберите файл ключа и введите пароль к нему",
    egovTitle = "Подпись в eGov mobile",
    egovScan = "Отсканируйте QR в eGov mobile на телефоне или откройте приложение на этом устройстве",
    egovOpen = "Открыть eGov mobile",
    egovNotOpened = "Не удалось открыть eGov mobile на этом " +
        "устройстве. Отсканируйте QR телефоном, где установлен eGov mobile",
    egovQr = "QR для подписи в eGov mobile",
    egovDocument = "Подпись для кабинета БФД — Суперкасса",
    keyTitle = "Подпись файлом ключа",
    keyOther = "Другой файл",
    keyPassword = "Пароль к ключу",
    showPassword = "Показать пароль",
    hidePassword = "Скрыть пароль",
    keySign = "Подписать",
    cancel = "Отмена",
    wrongPassword = "Пароль не подошёл. Проверьте раскладку и повторите",
    keyUnreadable = "Это не файл ключа ЭЦП или он повреждён. Выберите файл .p12",
    keyNotForSigning = "Это ключ для входа (AUTH). Выберите ключ для подписи — файл без AUTH в имени",
    keyExpired = "Срок сертификата ключа истёк. Выберите действующий ключ",
    egovUnreachable = "Служба подписи eGov mobile не отвечает. Проверьте интернет и повторите",
    egovExpired = "Время на подпись в eGov mobile вышло. Повторите",
    cancelled = "Подпись отменена"
)
