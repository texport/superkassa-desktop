package kz.mybrain.superkassa.domain.print.usecase

import kz.mybrain.superkassa.domain.print.model.asksPin
import kz.mybrain.superkassa.domain.signin.port.SignedKkm

/**
 * Пин кассы-рисовальщика надо спросить у владельца.
 *
 * Вошедший кассир свой пин уже отдал кассе на входе; владелец, открывший
 * кабинет с экрана входа, вводит пин ради формы отдельно.
 */
class AsksPin(private val signed: SignedKkm) {

    operator fun invoke(kkmId: String): Boolean = signed.asksPin(kkmId)
}
