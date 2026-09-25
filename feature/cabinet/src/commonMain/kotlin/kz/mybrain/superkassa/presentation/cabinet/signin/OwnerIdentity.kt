package kz.mybrain.superkassa.presentation.cabinet.signin

import kz.mybrain.superkassa.domain.cabinet.model.CabinetOwner
import kz.mybrain.superkassa.strings.api.cabinet.CabinetTexts

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
internal fun ownerIdentifier(owner: CabinetOwner, texts: CabinetTexts): String {
    val identifier = owner.company.bin
    val label = if (identifier.isNotBlank() && identifier == owner.user.iin) texts.signin.iin else texts.signin.bin
    return "$label $identifier"
}
