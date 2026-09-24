package kz.mybrain.superkassa.data.cabinet.setup

import kz.mybrain.superkassa.domain.cabinet.model.CabinetApplication
import kz.mybrain.superkassa.domain.cabinet.model.KkmRecord
import kz.mybrain.superkassa.domain.cabinet.model.documents.SignRequest
import kz.mybrain.superkassa.domain.cabinet.model.kkmRecord
import kz.mybrain.superkassa.domain.cabinet.port.CabinetPorts
import kz.mybrain.superkassa.domain.setup.model.CabinetRecord
import kz.mybrain.superkassa.domain.setup.model.RegistrationToSign
import kz.mybrain.superkassa.domain.setup.port.SetupCabinet

/**
 * Мастер подключения поверх портов кабинета окна.
 *
 * Своих обращений к кабинету у мастера нет: карточка кассы, заявление,
 * подпись и токен идут теми же портами, что и в разделах кабинета, —
 * с тем же вошедшим и тем же разбором отказов.
 */
class CabinetSetup(private val cabinet: CabinetPorts) : SetupCabinet {

    override suspend fun record(registerId: String): CabinetRecord =
        cabinet.registers.one(registerId).let {
            CabinetRecord(it.status, it.registrationNumber, awaiting = kkmRecord(it.status) == KkmRecord.Applied)
        }

    override suspend fun prepareRegistration(registerId: String): RegistrationToSign =
        cabinet.applications.prepare(CabinetApplication.Registration(registerId))
            .let { RegistrationToSign(it.actionId, it.payloadToSign) }

    override suspend fun sign(payload: String): String = cabinet.signer.sign(payload)

    override suspend fun sendRegistration(registerId: String, actionId: String, signature: String) {
        cabinet.applications.send(CabinetApplication.Registration(registerId), SignRequest(actionId, signature))
    }

    /** Токен, который кабинет так и не подтвердил, — не токен: заводить кассу с ним нельзя. */
    override suspend fun issueToken(registerId: String): String? =
        cabinet.registers.issueToken(registerId).token?.toString()
}
