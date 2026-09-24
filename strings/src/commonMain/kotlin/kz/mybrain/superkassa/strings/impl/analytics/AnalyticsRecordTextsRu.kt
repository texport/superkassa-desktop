package kz.mybrain.superkassa.strings.impl.analytics

import kz.mybrain.superkassa.strings.api.analytics.AnalyticsRecordTexts

/** Надписи [AnalyticsRecordTexts] по-русски. */
internal val analyticsRecordTextsRu = AnalyticsRecordTexts(
    tab = "Учёт касс",

    title = "Парк касс и его учёт",
    hint = "Кассу заводят в кабинете, а на учёт её ставит КГД по заявлению. Заведённая " +
        "касса — та, по которой заявление ещё не подавалось; поданное заявление ждёт " +
        "ответа КГД. Торговать по закону вправе только касса на учёте, и торгующие " +
        "считаются из них",

    total = "Всего касс",
    places = "Торговых точек",

    trading = "Сейчас торгуют",
    tradingOf = "%s из %s",
    blocked = "Заблокированы",

    refusals = "Отказы КГД",
    refusalsHint = "Кассы, которым КГД отказал в учёте. По каждой владелец разбирает причину " +
        "и подаёт заявление заново — сами они на учёт не встанут",
    refusalsNone = "Отказов КГД нет",

    regions = "Учёт по регионам",
    regionsHint = "Регион взят из адреса торговой точки: своего поля региона у кассы нет. " +
        "Регионы идут по числу касс — с самого крупного"
)
