package kz.mybrain.superkassa.domain.signin.usecase

import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.domain.workplace.port.WorkplaceMemory

/**
 * Переводит рабочее место на кассу: вход начинается заново — с пина этой
 * кассы. Та, за которой уже работают, заново не выбирается.
 */
class WorkOnKkm(private val signIn: SignIn, private val memory: WorkplaceMemory) {
    operator fun invoke(kkm: KkmResponse) {
        memory.rememberedKkmId = kkm.kkmId
        if (signIn.state.value.kkm?.kkmId != kkm.kkmId) signIn.switchKkm()
    }
}
