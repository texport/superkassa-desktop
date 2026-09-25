package kz.mybrain.superkassa.presentation.settings.kkm

import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.log.port.Journal
import kz.mybrain.superkassa.domain.settings.usecase.DecommissionKkm
import kz.mybrain.superkassa.domain.settings.usecase.RenameKkm
import kz.mybrain.superkassa.domain.settings.usecase.SwitchProgramming
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.domain.signin.usecase.ObserveSignIn
import kz.mybrain.superkassa.domain.signin.usecase.SwitchKkm
import kz.mybrain.superkassa.domain.workplace.port.WorkplaceChoices
import kz.mybrain.superkassa.domain.workplace.port.WorkplaceMemory
import kz.mybrain.superkassa.domain.workplace.usecase.ReadLocalName

/** Сценарии самой кассы в настройках. */
class KkmCases(kassa: Kassa, signIn: SignIn, workplace: WorkplaceChoices, memory: WorkplaceMemory, journal: Journal) {
    val observe = ObserveSignIn(signIn)
    val rename = RenameKkm(kassa, signIn, workplace)
    val programming = SwitchProgramming(kassa, signIn)
    val decommission = DecommissionKkm(kassa, signIn)
    val localName = ReadLocalName(memory)
    val switchKkm = SwitchKkm(signIn, journal)
}
