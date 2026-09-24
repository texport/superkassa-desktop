package kz.mybrain.superkassa.strings.impl.print

import kz.mybrain.superkassa.strings.api.print.PrintTexts

/** Надписи [PrintTexts] по-русски. */
internal val printTextsRu = PrintTexts(
    keepUnavailable = "На этом устройстве сохранить форму в файл пока нельзя: откройте её на другой машине",
    printerGone = "Принтер кассы не найден в системе: он отключён или удалён. Подключите его " +
        "или выберите другой принтер в настройках",
    systemDialog = "Печать идёт через системный диалог: принтер, число копий и «Сохранить как PDF» " +
        "выбираются в нём"
)
