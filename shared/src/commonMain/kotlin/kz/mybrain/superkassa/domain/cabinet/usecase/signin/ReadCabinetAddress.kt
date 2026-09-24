package kz.mybrain.superkassa.domain.cabinet.usecase.signin

import kz.mybrain.superkassa.domain.cabinet.port.CabinetAccount

/** Адрес кабинета: владелец видит, куда входит. */
class ReadCabinetAddress(private val account: CabinetAccount) {
    operator fun invoke(): String = account.address
}
