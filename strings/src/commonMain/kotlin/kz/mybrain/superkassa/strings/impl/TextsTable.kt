package kz.mybrain.superkassa.strings.impl

import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.Texts
import kz.mybrain.superkassa.strings.impl.analytics.analyticsTextsEn
import kz.mybrain.superkassa.strings.impl.analytics.analyticsTextsKk
import kz.mybrain.superkassa.strings.impl.analytics.analyticsTextsRu
import kz.mybrain.superkassa.strings.impl.cabinet.cabinetTextsEn
import kz.mybrain.superkassa.strings.impl.cabinet.cabinetTextsKk
import kz.mybrain.superkassa.strings.impl.cabinet.cabinetTextsRu
import kz.mybrain.superkassa.strings.impl.common.appStringsEn
import kz.mybrain.superkassa.strings.impl.common.appStringsKk
import kz.mybrain.superkassa.strings.impl.common.appStringsRu
import kz.mybrain.superkassa.strings.impl.debug.debugTextsEn
import kz.mybrain.superkassa.strings.impl.debug.debugTextsKk
import kz.mybrain.superkassa.strings.impl.debug.debugTextsRu
import kz.mybrain.superkassa.strings.impl.journal.journalTextsEn
import kz.mybrain.superkassa.strings.impl.journal.journalTextsKk
import kz.mybrain.superkassa.strings.impl.journal.journalTextsRu
import kz.mybrain.superkassa.strings.impl.kassa.kassaTextsEn
import kz.mybrain.superkassa.strings.impl.kassa.kassaTextsKk
import kz.mybrain.superkassa.strings.impl.kassa.kassaTextsRu
import kz.mybrain.superkassa.strings.impl.map.mapTextsEn
import kz.mybrain.superkassa.strings.impl.map.mapTextsKk
import kz.mybrain.superkassa.strings.impl.map.mapTextsRu
import kz.mybrain.superkassa.strings.impl.print.printTextsEn
import kz.mybrain.superkassa.strings.impl.print.printTextsKk
import kz.mybrain.superkassa.strings.impl.print.printTextsRu
import kz.mybrain.superkassa.strings.impl.settings.settingsTextsEn
import kz.mybrain.superkassa.strings.impl.settings.settingsTextsKk
import kz.mybrain.superkassa.strings.impl.settings.settingsTextsRu
import kz.mybrain.superkassa.strings.impl.setup.setupTextsEn
import kz.mybrain.superkassa.strings.impl.setup.setupTextsKk
import kz.mybrain.superkassa.strings.impl.setup.setupTextsRu
import kz.mybrain.superkassa.strings.impl.shell.startTextsEn
import kz.mybrain.superkassa.strings.impl.shell.startTextsKk
import kz.mybrain.superkassa.strings.impl.shell.startTextsRu
import kz.mybrain.superkassa.strings.impl.shift.coreTextsEn
import kz.mybrain.superkassa.strings.impl.shift.coreTextsKk
import kz.mybrain.superkassa.strings.impl.shift.coreTextsRu
import kz.mybrain.superkassa.strings.impl.update.updateTextsEn
import kz.mybrain.superkassa.strings.impl.update.updateTextsKk
import kz.mybrain.superkassa.strings.impl.update.updateTextsRu

/** Тексты на языке [language]: одна из трёх таблиц, собранных один раз. */
internal fun textsIn(language: Language): Texts = when (language) {
    Language.Kk -> kazakh
    Language.Ru -> russian
    Language.En -> english
}

private val kazakh = Texts(
    common = appStringsKk,
    analytics = analyticsTextsKk,
    cabinet = cabinetTextsKk,
    debug = debugTextsKk,
    journal = journalTextsKk,
    kassa = kassaTextsKk,
    map = mapTextsKk,
    print = printTextsKk,
    settings = settingsTextsKk,
    setup = setupTextsKk,
    shell = startTextsKk,
    shift = coreTextsKk,
    update = updateTextsKk
)

private val russian = Texts(
    common = appStringsRu,
    analytics = analyticsTextsRu,
    cabinet = cabinetTextsRu,
    debug = debugTextsRu,
    journal = journalTextsRu,
    kassa = kassaTextsRu,
    map = mapTextsRu,
    print = printTextsRu,
    settings = settingsTextsRu,
    setup = setupTextsRu,
    shell = startTextsRu,
    shift = coreTextsRu,
    update = updateTextsRu
)

private val english = Texts(
    common = appStringsEn,
    analytics = analyticsTextsEn,
    cabinet = cabinetTextsEn,
    debug = debugTextsEn,
    journal = journalTextsEn,
    kassa = kassaTextsEn,
    map = mapTextsEn,
    print = printTextsEn,
    settings = settingsTextsEn,
    setup = setupTextsEn,
    shell = startTextsEn,
    shift = coreTextsEn,
    update = updateTextsEn
)
