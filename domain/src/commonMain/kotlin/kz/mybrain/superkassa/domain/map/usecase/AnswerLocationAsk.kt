package kz.mybrain.superkassa.domain.map.usecase

import kz.mybrain.superkassa.domain.map.model.SelfPlace
import kz.mybrain.superkassa.domain.map.port.MapMemory
import kz.mybrain.superkassa.domain.map.port.Maps

/**
 * Владелец ответил на вопрос о запасном пути.
 *
 * Разрешение помнится, и больше его не спрашивают: место ищется сразу.
 * Запретил — наружу ничего не уходит, а спросят его снова только по
 * следующему нажатию «Где я», см. [LocateSelf].
 */
class AnswerLocationAsk(private val maps: Maps, private val memory: MapMemory) {

    suspend operator fun invoke(allowed: Boolean): SelfPlace {
        memory.locationAllowed = allowed
        return if (allowed) byConnection(maps) else SelfPlace.Unknown
    }
}
