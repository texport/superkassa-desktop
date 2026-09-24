package kz.mybrain.superkassa.presentation.kassa.refund

import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.kassa.usecase.IssueRefund
import kz.mybrain.superkassa.domain.kassa.usecase.ReadBasisItems
import kz.mybrain.superkassa.domain.kassa.usecase.ReadReturnDay
import kz.mybrain.superkassa.domain.kassa.usecase.ReadTillReference
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.domain.signin.usecase.ObserveSignIn
import kz.mybrain.superkassa.domain.workplace.port.WorkplaceMemory

/**
 * Сценарии возврата: всё, что модель делает с кассой и памятью места, — через них.
 *
 * Касса и пин команды — у сценариев, модель их не видит.
 */
class RefundCases(kassa: Kassa, signIn: SignIn, memory: WorkplaceMemory) {
    val observe = ObserveSignIn(signIn)
    val readDay = ReadReturnDay(kassa, signIn, memory)
    val readItems = ReadBasisItems(kassa, signIn)
    val reference = ReadTillReference(kassa)
    val issue = IssueRefund(kassa, signIn)
}
