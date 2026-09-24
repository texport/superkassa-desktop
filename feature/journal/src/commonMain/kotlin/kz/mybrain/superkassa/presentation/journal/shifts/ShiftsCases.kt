package kz.mybrain.superkassa.presentation.journal.shifts

import kz.mybrain.superkassa.domain.document.usecase.ReadDocumentTypes
import kz.mybrain.superkassa.domain.journal.usecase.ReadShiftDocuments
import kz.mybrain.superkassa.domain.journal.usecase.ReadShifts
import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.domain.signin.usecase.ObserveSignIn

/** Сценарии прошлых смен: смены кассы страницами и документы открытой из них. */
class ShiftsCases(kassa: Kassa, signIn: SignIn) {
    val observe = ObserveSignIn(signIn)
    val readDocumentTypes = ReadDocumentTypes(kassa)
    val readShifts = ReadShifts(kassa, signIn)
    val readShiftDocuments = ReadShiftDocuments(kassa, signIn)
}
