package kz.mybrain.superkassa.domain.setup.usecase

import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.model.ask
import kz.mybrain.superkassa.domain.kassa.model.map
import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.setup.model.KkmSetupDraft
import kz.mybrain.superkassa.domain.setup.port.SetupMemory

/**
 * Выдаёт заводской номер и запоминает его в пройденном.
 *
 * Касса выдаёт новый номер на каждый запрос: владелец, открывший мастер
 * дважды, уносил в кабинет один номер, а видел потом другой. Поэтому
 * номер, уже выданный, второй раз не спрашивается.
 */
class GetFactoryNumber(private val kassa: Kassa, private val memory: SetupMemory) {

    /** @return пройденное с номером; `null` — номер уже выдан. */
    suspend operator fun invoke(draft: KkmSetupDraft): Answer<KkmSetupDraft>? {
        if (draft.factoryNumber != null) return null
        return kassa.ask { it.generateFactoryInfo() }.map { factory ->
            draft.copy(factoryNumber = factory.factoryNumber, manufactureYear = factory.manufactureYear.toString())
                .also { it.saveTo(memory) }
        }
    }
}
