package kz.mybrain.superkassa.domain.kassa.port

import io.github.texport.superkassa.core.domain.api.model.settings.DeliverySettings

/**
 * Настройки доставки чека, как их держит ядро.
 *
 * Касса спрашивает их, чтобы предложить кассиру только те виды контакта
 * покупателя, по которым чек действительно уйдёт. Правит их раздел
 * настроек, касса только читает.
 */
interface DeliverySetup {

    /** Действующие настройки доставки; `null` — доставка не настраивалась. */
    suspend fun read(): DeliverySettings?
}
