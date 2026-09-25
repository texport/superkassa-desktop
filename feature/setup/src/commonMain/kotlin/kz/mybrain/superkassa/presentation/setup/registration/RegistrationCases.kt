package kz.mybrain.superkassa.presentation.setup.registration

import kz.mybrain.superkassa.domain.setup.port.SetupCabinet
import kz.mybrain.superkassa.domain.setup.usecase.ReadCabinetRecord
import kz.mybrain.superkassa.domain.setup.usecase.SubmitRegistration

/** Сценарии шага постановки на учёт: касса в кабинете и заявление о ней. */
internal class RegistrationCases(cabinet: SetupCabinet) {
    val readRecord = ReadCabinetRecord(cabinet)
    val submit = SubmitRegistration(cabinet)
}
