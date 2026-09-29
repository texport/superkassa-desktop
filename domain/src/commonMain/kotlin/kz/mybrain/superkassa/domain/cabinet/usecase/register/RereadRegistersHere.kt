package kz.mybrain.superkassa.domain.cabinet.usecase.register

import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmListParams
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.domain.cabinet.port.CabinetRegisters
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.model.answering
import kz.mybrain.superkassa.domain.kassa.model.ask
import kz.mybrain.superkassa.domain.kassa.port.Kassa

/**
 * Свежие карточки касс этой машины из показанного списка кабинета.
 *
 * Кассу этой машины меняют и вне кабинета: мастер подключения ставит её
 * на учёт, и в прочитанном раньше списке она так и оставалась черновиком.
 * Весь список сети — сотни обращений, а касс машины единицы: перечитываются
 * только они.
 *
 * @return перечитанные карточки; касса, которую прочитать не удалось,
 *   остаётся в списке прежней.
 */
class RereadRegistersHere(private val kassa: Kassa, private val registers: CabinetRegisters) {
    suspend operator fun invoke(shown: List<CabinetRegister>): List<CabinetRegister> {
        val listed = kassa.ask { it.listKkms(KkmListParams(limit = MAX_KKMS)) } as? Answer.Done ?: return emptyList()
        val here = listed.value.items.mapNotNull { it.ofdSystemId }.toSet()
        return shown.filter { it.kkmId.toString() in here }
            .mapNotNull { (answering { registers.one(it.id) } as? Answer.Done)?.value }
    }

    private companion object {
        /** Сколько касс этой машины читать разом: больше не бывает и у сети. */
        const val MAX_KKMS = 1000
    }
}
