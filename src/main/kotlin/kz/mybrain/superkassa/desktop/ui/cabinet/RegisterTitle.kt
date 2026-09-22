package kz.mybrain.superkassa.desktop.ui.cabinet

import kz.mybrain.superkassa.desktop.server.cabinet.CabinetRegister

/**
 * Как назвать кассу.
 *
 * Своё название владельца, а без него — номер учёта, а без него —
 * заводской: только что заведённая касса не имеет ни того, ни другого,
 * и безымянной строки в списке быть не должно.
 */
fun registerTitle(register: CabinetRegister): String =
    register.internalName?.takeIf { it.isNotBlank() }
        ?: register.registrationNumber
        ?: register.factoryNumber.orEmpty()
