package kz.mybrain.superkassa.strings.impl.cabinet

import kz.mybrain.superkassa.strings.api.cabinet.CabinetTexts
import kz.mybrain.superkassa.strings.impl.cabinet.address.addressTextsKk
import kz.mybrain.superkassa.strings.impl.cabinet.applications.applicationTextsKk
import kz.mybrain.superkassa.strings.impl.cabinet.company.companyTextsKk
import kz.mybrain.superkassa.strings.impl.cabinet.documents.documentTextsKk
import kz.mybrain.superkassa.strings.impl.cabinet.eds.edsTextsKk
import kz.mybrain.superkassa.strings.impl.cabinet.enroll.enrollTextsKk
import kz.mybrain.superkassa.strings.impl.cabinet.machine.machineTextsKk
import kz.mybrain.superkassa.strings.impl.cabinet.places.placeTextsKk
import kz.mybrain.superkassa.strings.impl.cabinet.refusal.cabinetRefusalTextsKk
import kz.mybrain.superkassa.strings.impl.cabinet.register.registerTextsKk
import kz.mybrain.superkassa.strings.impl.cabinet.signin.signInTextsKk
import kz.mybrain.superkassa.strings.impl.map.mapTextsKk

/** Надписи [CabinetTexts] по-казахски. */
internal val cabinetTextsKk = CabinetTexts(
    title = "БФД жеке кабинеті",
    refresh = "Жаңарту",
    add = "Қосу",
    remove = "Алып тастау",
    save = "Сақтау",
    close = "Жабу",
    required = "Міндетті",
    optional = "Міндетті емес",
    missing = "Толтырылмаған",
    signin = signInTextsKk,
    refusal = cabinetRefusalTextsKk,
    company = companyTextsKk,
    places = placeTextsKk,
    address = addressTextsKk,
    enroll = enrollTextsKk,
    register = registerTextsKk,
    applications = applicationTextsKk,
    documents = documentTextsKk,
    statuses = cabinetStatusTextsKk,
    map = mapTextsKk,
    hints = cabinetHintTextsKk,
    eds = edsTextsKk,
    machine = machineTextsKk
)
