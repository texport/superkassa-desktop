package kz.mybrain.superkassa.strings.impl.cabinet

import kz.mybrain.superkassa.strings.api.cabinet.CabinetTexts
import kz.mybrain.superkassa.strings.impl.cabinet.address.addressTextsEn
import kz.mybrain.superkassa.strings.impl.cabinet.applications.applicationTextsEn
import kz.mybrain.superkassa.strings.impl.cabinet.company.companyTextsEn
import kz.mybrain.superkassa.strings.impl.cabinet.documents.documentTextsEn
import kz.mybrain.superkassa.strings.impl.cabinet.eds.edsTextsEn
import kz.mybrain.superkassa.strings.impl.cabinet.enroll.enrollTextsEn
import kz.mybrain.superkassa.strings.impl.cabinet.machine.machineTextsEn
import kz.mybrain.superkassa.strings.impl.cabinet.places.placeTextsEn
import kz.mybrain.superkassa.strings.impl.cabinet.refusal.cabinetRefusalTextsEn
import kz.mybrain.superkassa.strings.impl.cabinet.register.registerTextsEn
import kz.mybrain.superkassa.strings.impl.cabinet.signin.signInTextsEn
import kz.mybrain.superkassa.strings.impl.map.mapTextsEn

/** Надписи [CabinetTexts] по-английски. */
internal val cabinetTextsEn = CabinetTexts(
    title = "BFD cabinet",
    refresh = "Refresh",
    add = "Add",
    remove = "Remove",
    save = "Save",
    close = "Close",
    required = "Required",
    optional = "Optional",
    missing = "Not filled in",
    signin = signInTextsEn,
    refusal = cabinetRefusalTextsEn,
    company = companyTextsEn,
    places = placeTextsEn,
    address = addressTextsEn,
    enroll = enrollTextsEn,
    register = registerTextsEn,
    applications = applicationTextsEn,
    documents = documentTextsEn,
    statuses = cabinetStatusTextsEn,
    map = mapTextsEn,
    hints = cabinetHintTextsEn,
    eds = edsTextsEn,
    machine = machineTextsEn
)
