package kz.mybrain.superkassa.presentation.cabinet.signing

import kotlinx.coroutines.flow.StateFlow
import kz.mybrain.superkassa.domain.cabinet.model.signature.SignAnswer
import kz.mybrain.superkassa.domain.cabinet.model.signature.SignMethod
import kz.mybrain.superkassa.domain.cabinet.model.signature.SignRequest

/**
 * Подпись кабинета окна: чем подписывать и что подписывающий просит сейчас.
 *
 * Часть модели кабинета, а не своя модель: способ выбирают у кнопки входа,
 * а просьбу подписывающего показывает окно над всем приложением, и обоим
 * нужна одна и та же подпись окна.
 */
class CabinetSigning internal constructor(private val cases: SigningCases) {

    /** Способы этой платформы; один — выбирать нечего. */
    val methods: List<SignMethod> = cases.methods()

    /** Выбранный способ. */
    val method: StateFlow<SignMethod> = cases.method()

    /** Что показать владельцу сейчас; `null` — ничего. */
    val request: StateFlow<SignRequest?> = cases.request()

    /** Есть из чего выбирать. */
    val choosable: Boolean get() = methods.size > 1

    /** Владелец выбирает способ подписи. */
    fun choose(method: SignMethod) = cases.choose(method)

    /** Ответ владельца на просьбу подписывающего. */
    fun answer(answer: SignAnswer) = cases.answer(answer)
}
