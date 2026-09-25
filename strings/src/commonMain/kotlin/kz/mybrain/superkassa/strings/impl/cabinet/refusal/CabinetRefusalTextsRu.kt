package kz.mybrain.superkassa.strings.impl.cabinet.refusal

import kz.mybrain.superkassa.strings.api.cabinet.refusal.CabinetRefusalTexts

/** Надписи [CabinetRefusalTexts] по-русски. */
internal val cabinetRefusalTextsRu = CabinetRefusalTexts(
    unreachable = "Кабинет не отвечает по заданному адресу",
    unreadable = "Кабинет ответил не так, как приложение умеет прочитать",
    sessionExpired = "Доступ истёк — войдите заново",
    noNcaLayer = "NCALayer не отвечает. Запустите его и повторите вход",
    signDeclined = "Подпись не получена",
    signWindowClosed = "Окно подписи закрыто",
    signCancelled = "Подпись отменена в NCALayer",
    kkmNotActive = "Касса не допущена к обслуживанию на сервере приёма данных",
    defaultPinNotAllowed = "У кассы стоит пин по умолчанию — смените его в самой кассе"
)
