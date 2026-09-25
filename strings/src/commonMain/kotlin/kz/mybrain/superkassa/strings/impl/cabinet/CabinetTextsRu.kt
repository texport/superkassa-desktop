package kz.mybrain.superkassa.strings.impl.cabinet

import kz.mybrain.superkassa.strings.api.cabinet.CabinetTexts
import kz.mybrain.superkassa.strings.impl.cabinet.address.addressTextsRu
import kz.mybrain.superkassa.strings.impl.cabinet.applications.applicationTextsRu
import kz.mybrain.superkassa.strings.impl.cabinet.company.companyTextsRu
import kz.mybrain.superkassa.strings.impl.cabinet.documents.documentTextsRu
import kz.mybrain.superkassa.strings.impl.cabinet.eds.edsTextsRu
import kz.mybrain.superkassa.strings.impl.cabinet.enroll.enrollTextsRu
import kz.mybrain.superkassa.strings.impl.cabinet.machine.machineTextsRu
import kz.mybrain.superkassa.strings.impl.cabinet.places.placeTextsRu
import kz.mybrain.superkassa.strings.impl.cabinet.refusal.cabinetRefusalTextsRu
import kz.mybrain.superkassa.strings.impl.cabinet.register.registerTextsRu
import kz.mybrain.superkassa.strings.impl.cabinet.signin.signInTextsRu
import kz.mybrain.superkassa.strings.impl.map.mapTextsRu

/** Надписи [CabinetTexts] по-русски. */
internal val cabinetTextsRu = CabinetTexts(
    title = "Кабинет БФД",
    refresh = "Обновить",
    add = "Добавить",
    remove = "Убрать",
    save = "Сохранить",
    close = "Закрыть",
    required = "Обязательно",
    optional = "Необязательно",
    missing = "Не заполнено",
    signin = signInTextsRu,
    refusal = cabinetRefusalTextsRu,
    company = companyTextsRu,
    places = placeTextsRu,
    address = addressTextsRu,
    enroll = enrollTextsRu,
    register = registerTextsRu,
    applications = applicationTextsRu,
    documents = documentTextsRu,
    statuses = cabinetStatusTextsRu,
    map = mapTextsRu,
    hints = cabinetHintTextsRu,
    eds = edsTextsRu,
    machine = machineTextsRu
)
