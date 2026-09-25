package kz.mybrain.superkassa.strings.impl.cabinet

import kz.mybrain.superkassa.strings.api.cabinet.CabinetStatusTexts

/** Надписи [CabinetStatusTexts] по-русски. */
internal val cabinetStatusTextsRu = CabinetStatusTexts(
    draft = "Черновик",
    registered = "На учёте",
    deregistered = "Снята с учёта",
    active = "Работает",
    inactive = "Остановлена",
    blocked = "Заблокирована",
    accepted = "Принято",
    rejected = "Отказано",
    sent = "Отправлено",
    inProcess = "На рассмотрении в КГД",
    shiftOpen = "Смена открыта",
    shiftClosed = "Смена закрыта",
    unknown = "Состояние неизвестно"
)
