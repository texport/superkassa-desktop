package kz.mybrain.superkassa.domain.shift.usecase

import io.github.texport.superkassa.core.presentation.api.model.ofd.OfdCommandStatus
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.model.map
import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.signin.model.askSeated
import kz.mybrain.superkassa.domain.signin.port.SignedKkm

/** Есть ли связь с БФД: `true` — ответил, `false` — касса спросила, а ответа нет. */
class CheckOfdLink(private val kassa: Kassa, private val signed: SignedKkm) {
    suspend operator fun invoke(): Answer<Boolean> =
        kassa.askSeated(signed) { api, seat -> api.checkOfdConnection(seat.kkmId) }
            .map { it.status == OfdCommandStatus.OK }
}
