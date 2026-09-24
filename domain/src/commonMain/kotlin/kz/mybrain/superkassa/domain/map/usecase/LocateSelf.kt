package kz.mybrain.superkassa.domain.map.usecase

import kz.mybrain.superkassa.domain.map.model.SelfPlace
import kz.mybrain.superkassa.domain.map.port.MapMemory
import kz.mybrain.superkassa.domain.map.port.Maps

/**
 * «Где я»: сперва служба геопозиции самой машины, потом — адрес подключения.
 *
 * Своя служба точна до дома, и разрешение на неё спрашивает система своим
 * окном. Запасной путь отдаёт чужой службе адрес подключения — это решение
 * владельца, и разрешение помнится рабочим местом: разрешил — место ищется
 * сразу. Не решал или запретил — наружу ничего не уходит, а владельца
 * спрашивают снова: определение начинается только с его нажатия на «Где я»,
 * и запомненный отказ делал эту кнопку мёртвой навсегда — передумать было
 * негде.
 */
class LocateSelf(private val maps: Maps, private val memory: MapMemory) {

    suspend operator fun invoke(): SelfPlace {
        maps.locateMachine()?.let { return SelfPlace.Found(it, precise = true) }
        return when (memory.locationAllowed) {
            true -> byConnection(maps)
            false, null -> SelfPlace.AskOwner
        }
    }
}

/** Город по адресу подключения: точность — до города поставщика связи. */
internal suspend fun byConnection(maps: Maps): SelfPlace =
    maps.locateByConnection()?.let { SelfPlace.Found(it, precise = false) } ?: SelfPlace.Unknown
