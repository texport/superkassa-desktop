package kz.mybrain.superkassa.strings.impl.share

import kz.mybrain.superkassa.strings.api.common.ShareTexts

/** Надписи [ShareTexts] по-русски. */
internal val shareTextsRu = ShareTexts(
    share = "Поделиться",
    whatsApp = "WhatsApp",
    telegram = "Telegram",
    email = "Почта",
    subject = "Электронный чек",
    withLink = "Электронный чек: %s",
    withoutLink = "Электронный чек — во вложении",
    noLink = "Ссылки на чек ещё нет: БФД его пока не принял. " +
        "Сохраните файл и отправьте его покупателю сами",
    failed = "Не удалось открыть программу для отправки. Сохраните файл и отправьте его покупателю сами"
)
