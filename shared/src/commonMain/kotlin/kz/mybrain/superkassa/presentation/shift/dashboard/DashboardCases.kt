package kz.mybrain.superkassa.presentation.shift.dashboard

import kz.mybrain.superkassa.domain.document.usecase.ReadDocumentTypes
import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.shift.usecase.CheckOfdLink
import kz.mybrain.superkassa.domain.shift.usecase.CloseShift
import kz.mybrain.superkassa.domain.shift.usecase.OpenShift
import kz.mybrain.superkassa.domain.shift.usecase.ReadRefusedOperators
import kz.mybrain.superkassa.domain.shift.usecase.ReadShift
import kz.mybrain.superkassa.domain.shift.usecase.SendQueued
import kz.mybrain.superkassa.domain.shift.usecase.TakeXReport
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.domain.signin.usecase.ObserveSignIn

/**
 * Сценарии главного экрана: всё, что модель делает с кассой, — только через них.
 *
 * Собираются из портов один раз на модель; проверки подают порты на
 * тестовом ядре. Касса и пин команды — у сценариев, модель их не видит.
 */
class DashboardCases(kassa: Kassa, signIn: SignIn) {
    val observe = ObserveSignIn(signIn)
    val readShift = ReadShift(kassa, signIn)
    val readOperators = ReadRefusedOperators(kassa, signIn)
    val readDocumentTypes = ReadDocumentTypes(kassa)
    val openShift = OpenShift(kassa, signIn)
    val closeShift = CloseShift(kassa, signIn)
    val xReport = TakeXReport(kassa, signIn)
    val checkLink = CheckOfdLink(kassa, signIn)
    val sendQueued = SendQueued(kassa, signIn)
}
