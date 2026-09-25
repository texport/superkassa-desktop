package kz.mybrain.superkassa.domain.cabinet.usecase.signature

import kz.mybrain.superkassa.domain.cabinet.model.signature.SignAnswer
import kz.mybrain.superkassa.domain.cabinet.port.Signing

/** Ответ владельца подписывающему: пароль, другой файл или отмена. */
class AnswerSignRequest(private val signing: Signing) {
    operator fun invoke(answer: SignAnswer) = signing.desk.answer(answer)
}
