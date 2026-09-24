package kz.mybrain.superkassa.domain.setup.usecase

import kz.mybrain.superkassa.domain.setup.model.KkmSetupDraft
import kz.mybrain.superkassa.domain.setup.port.SetupMemory

/**
 * Пройденное мастером: прочитать, каким его оставили, и запомнить новое.
 *
 * Токен кассы сюда не попадает никогда — его в пройденном нет.
 */
class RememberSetup(private val memory: SetupMemory) {

    fun read(): KkmSetupDraft = KkmSetupDraft.readFrom(memory)

    /** Запоминает [draft] и отдаёт его же. */
    operator fun invoke(draft: KkmSetupDraft): KkmSetupDraft = draft.also { it.saveTo(memory) }

    /** Касса в кабинете заведена: запоминается с идентификатором у БФД и названием. */
    fun register(draft: KkmSetupDraft, id: String, kkmId: Int, name: String?): KkmSetupDraft =
        invoke(draft.copy(cabinetRegisterId = id, systemId = kkmId.toString(), name = name?.takeIf { it.isNotBlank() }))
}
