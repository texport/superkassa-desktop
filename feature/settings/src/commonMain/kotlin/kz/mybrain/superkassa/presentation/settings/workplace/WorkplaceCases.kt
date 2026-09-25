package kz.mybrain.superkassa.presentation.settings.workplace

import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.domain.signin.usecase.ObserveSignIn
import kz.mybrain.superkassa.domain.workplace.port.WorkplaceChoices
import kz.mybrain.superkassa.domain.workplace.port.WorkplaceMemory
import kz.mybrain.superkassa.domain.workplace.usecase.ChooseTradeDomain
import kz.mybrain.superkassa.domain.workplace.usecase.ReadWorkplace
import kz.mybrain.superkassa.domain.workplace.usecase.SaveCabinetAddress
import kz.mybrain.superkassa.domain.workplace.usecase.SaveCabinetServer
import kz.mybrain.superkassa.domain.workplace.usecase.SaveMapServices

/** Сценарии настроек машины. */
internal class WorkplaceCases(signIn: SignIn, choices: WorkplaceChoices, memory: WorkplaceMemory) {
    val observe = ObserveSignIn(signIn)
    val read = ReadWorkplace(choices, memory)
    val saveCabinet = SaveCabinetAddress(choices)
    val saveServer = SaveCabinetServer(choices)
    val saveMaps = SaveMapServices(choices)
    val chooseDomain = ChooseTradeDomain(choices)
}
