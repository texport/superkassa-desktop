package kz.mybrain.superkassa.domain.cabinet.usecase.card

import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.domain.cabinet.port.CabinetCards
import kz.mybrain.superkassa.domain.cabinet.port.SavedFiles

/**
 * Сохраняет регистрационную карту в PDF — действующую или названную версию.
 *
 * @return имя сохранённого файла; `null` — владелец передумал.
 */
class SaveCardPdf(private val cards: CabinetCards, private val files: SavedFiles) {
    suspend operator fun invoke(register: CabinetRegister, version: Int? = null): String? {
        val bytes = if (version == null) cards.cardPdf(register.id) else cards.versionPdf(register.id, version)
        val suffix = version?.let { "-v$it" }.orEmpty()
        return files.save(bytes, "registration-card-${register.kkmId}$suffix.pdf")
    }
}
