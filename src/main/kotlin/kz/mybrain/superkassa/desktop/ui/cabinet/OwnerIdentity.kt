package kz.mybrain.superkassa.desktop.ui.cabinet

import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts

/**
 * Как назван идентификатор владельца: БИН или ИИН.
 *
 * У товарищества это БИН, а индивидуальный предприниматель действует
 * под своим ИИН — отдельного БИН у него нет. Кабинет хранит и то и другое
 * в одном поле, и опознать случай можно единственным верным способом:
 * у ИП идентификатор компании совпадает с ИИН вошедшего.
 *
 * Подписать ИИН словом «БИН» — не мелочь: это чужой реквизит, и в чеке
 * с ним разбираются налоговая и покупатель.
 */
fun ownerIdentifier(cabinet: CabinetSession, texts: CabinetTexts): String {
    val identifier = cabinet.company?.bin.orEmpty()
    val label = if (identifier.isNotBlank() && identifier == cabinet.user?.iin) texts.iin else texts.bin
    return "$label $identifier"
}
