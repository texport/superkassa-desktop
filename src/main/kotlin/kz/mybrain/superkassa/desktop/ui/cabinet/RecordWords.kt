package kz.mybrain.superkassa.desktop.ui.cabinet

import kz.mybrain.superkassa.desktop.ui.strings.SieveTexts

/**
 * Смысл учёта КГД словами — одними и теми же во всём приложении.
 *
 * Названо здесь, рядом с самим перечислением [KkmRecord], а не в разделе,
 * который спросил первым: смыслы учёта спрашивают карта аналитики,
 * плитки учёта и колонка торговых точек, и владелец обязан читать
 * в них одно и то же слово о одном и том же состоянии кассы.
 *
 * `null` — учёт не спрошен вовсе: это шестой пункт отбора, а не шестое
 * состояние кассы.
 */
fun recordTitle(record: KkmRecord?, texts: SieveTexts): String = when (record) {
    null -> texts.allRecords
    KkmRecord.OnRecord -> texts.onRecord
    KkmRecord.Entered -> texts.entered
    KkmRecord.Applied -> texts.applied
    KkmRecord.Refused -> texts.refused
    KkmRecord.Deregistered -> texts.deregistered
}
