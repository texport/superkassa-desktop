package kz.mybrain.superkassa.presentation.kassa.sale

import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.kassa.port.KassaPorts
import kz.mybrain.superkassa.domain.kassa.usecase.IssueSale
import kz.mybrain.superkassa.domain.kassa.usecase.LookupGoods
import kz.mybrain.superkassa.domain.kassa.usecase.ReadCollapsedPanels
import kz.mybrain.superkassa.domain.kassa.usecase.ReadContactChannels
import kz.mybrain.superkassa.domain.kassa.usecase.ReadSaleSeat
import kz.mybrain.superkassa.domain.kassa.usecase.ReadTillReference
import kz.mybrain.superkassa.domain.kassa.usecase.RememberCollapsedPanels
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.domain.signin.usecase.ObserveSignIn
import kz.mybrain.superkassa.domain.workplace.port.WorkplaceMemory

/**
 * Сценарии продажи: всё, что модель делает с кассой и памятью места, — через них.
 *
 * Собираются из портов один раз на модель; проверки подают порты на
 * тестовом ядре. Касса и пин команды — у сценариев, модель их не видит.
 */
class SaleCases(kassa: Kassa, signIn: SignIn, memory: WorkplaceMemory, ports: KassaPorts) {
    val observe = ObserveSignIn(signIn)
    val issue = IssueSale(kassa, signIn)
    val lookup = LookupGoods(kassa, signIn)
    val reference = ReadTillReference(kassa)
    val seat = ReadSaleSeat(kassa, signIn, memory)
    val panels = ReadCollapsedPanels(memory)
    val rememberPanels = RememberCollapsedPanels(memory)
    val channels = ReadContactChannels(ports.delivery)
}
