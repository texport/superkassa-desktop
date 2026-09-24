package kz.mybrain.superkassa.data.cabinet

import kz.mybrain.superkassa.domain.cabinet.port.Signer
import kz.mybrain.superkassa.domain.log.port.Journal
import kz.mybrain.superkassa.integrations.bfdcabinet.CabinetJournal
import kz.mybrain.superkassa.integrations.bfdcabinet.CabinetSigner

/** Подписывающий владельца — тем, кого модуль кабинета просит подписать задачу входа. */
internal fun signing(signer: Signer): CabinetSigner = CabinetSigner { payload -> signer.sign(payload) }

/**
 * Журнал обмена модуля — журналом приложения.
 *
 * Строки модуля уже без тел и доступа; помеха сети пишется её именем:
 * у отказов защищённого соединения сообщение бывает пустым.
 */
internal fun exchange(journal: Journal): CabinetJournal = CabinetJournal { line, failure ->
    if (failure == null) {
        journal.info("cabinet: $line")
    } else {
        journal.warn("cabinet: $line, ${failure::class.simpleName}")
    }
}
