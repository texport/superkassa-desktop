package kz.mybrain.superkassa.domain.kassa.usecase

import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.model.ContactChannels
import kz.mybrain.superkassa.domain.kassa.model.answering
import kz.mybrain.superkassa.domain.kassa.port.DeliverySetup

/**
 * Какими видами контакта можно отправить чек покупателю.
 *
 * Молча: не прочитанные настройки — не беда кассира. Отправлять тогда
 * некуда, и чек показывают покупателю или печатают, как без доставки.
 */
class ReadContactChannels(private val setup: DeliverySetup) {

    suspend operator fun invoke(): ContactChannels =
        ContactChannels.of((answering { setup.read() } as? Answer.Done)?.value)
}
