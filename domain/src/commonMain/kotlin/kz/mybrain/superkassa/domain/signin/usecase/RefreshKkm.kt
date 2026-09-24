package kz.mybrain.superkassa.domain.signin.usecase

import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.model.ask
import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.signin.port.SignedKkm

/**
 * Перечитывает кассу, за которой работают, и отдаёт её всем разделам.
 *
 * Сверка с БФД, замена токена и фискальная операция меняют кассу, а ответом
 * её не отдают. Не прочиталась — прежнее состояние остаётся: команда уже
 * выполнена, и беда перечитывания её итога не отменяет.
 */
class RefreshKkm(private val kassa: Kassa, private val signed: SignedKkm) {

    /** @return касса как она есть; `null` — за кассой никто не работает или она не ответила. */
    suspend operator fun invoke(): KkmResponse? {
        val kkmId = signed.seat()?.kkmId ?: return null
        return (kassa.ask { it.getKkm(kkmId) } as? Answer.Done)?.value?.also(signed::refresh)
    }
}
