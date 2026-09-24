package kz.mybrain.superkassa.domain.signin.usecase

import kz.mybrain.superkassa.domain.log.port.Journal
import kz.mybrain.superkassa.domain.signin.model.SignIn

/**
 * Уводит на выбор кассы: за этой машиной будет работать другая.
 *
 * Смена кассы — не переключатель на месте: пин принадлежит той кассе,
 * в которую по нему вошли, и вход начинается заново — иначе кассир менял
 * бы кассу, не заметив, и пробивал чек не на той машине.
 */
class SwitchKkm(private val signIn: SignIn, private val journal: Journal) {
    suspend operator fun invoke() {
        val kkm = signIn.state.value.kkm ?: return
        journal.info("left kkm ${kkm.kkmId} for kkm choice")
        signIn.switchKkm()
    }
}
