package kz.mybrain.superkassa.data.eds

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kz.mybrain.superkassa.data.local.ForegroundActivity
import kz.mybrain.superkassa.domain.cabinet.model.EdsProblem
import kz.mybrain.superkassa.domain.cabinet.model.EdsRefusal
import kz.mybrain.superkassa.domain.cabinet.model.signature.KeyProblem
import kz.mybrain.superkassa.domain.cabinet.model.signature.SignAnswer
import kz.mybrain.superkassa.domain.cabinet.model.signature.SignDesk
import kz.mybrain.superkassa.domain.cabinet.model.signature.SignRequest
import kz.mybrain.superkassa.domain.cabinet.port.Signer
import kz.mybrain.superkassa.integrations.kalkan.KalkanReason
import kz.mybrain.superkassa.integrations.kalkan.KalkanRefusal
import kz.mybrain.superkassa.integrations.kalkan.KeyFileSigner
import kotlin.io.encoding.Base64

/**
 * Подпись файлом ключа `.p12` на этом устройстве — провайдером Kalkan.
 *
 * Файл владелец выбирает системным окном, пароль — вводит на столе подписи
 * при каждой подписи. Неудача с файлом (не тот пароль, не ключ, ключ
 * для входа, просроченный) подпись не обрывает: владелец видит её в том же
 * окне и пробует снова. Обрывает только отказ владельца.
 *
 * Выбранный файл помнится, пока живёт приложение, — второй раз его
 * не ищут; пароль не помнится нигде и затирается сразу после попытки.
 */
class KeyFileSigning internal constructor(private val files: KeyFiles, private val desk: SignDesk) : Signer {
    private var chosen: KeyFile? = null

    /** Файл — системным окном поверх [screen]. */
    constructor(screen: ForegroundActivity, desk: SignDesk) : this(KeyFilePicker(screen), desk)

    override suspend fun sign(payload: String): String {
        var file = chosen ?: files.pick() ?: throw cancelled()
        var problem: KeyProblem? = null
        while (true) {
            when (val answer = desk.ask(SignRequest.KeyPassword(file.name, problem))) {
                SignAnswer.Cancel -> throw cancelled()
                SignAnswer.OtherFile -> file = (files.pick() ?: file).also { problem = null }
                is SignAnswer.Password -> {
                    val attempt = attempt(file, payload, answer.chars)
                    attempt.signature?.let { return it.also { chosen = file } }
                    problem = attempt.problem
                }
            }
        }
    }

    /** Одна попытка подписи; пароль затирается в любом исходе. */
    private suspend fun attempt(file: KeyFile, payload: String, password: CharArray): Attempt = try {
        val content = Base64.decode(payload)
        val cms = withContext(Dispatchers.Default) { KeyFileSigner.sign(file.bytes, password, content) }
        Attempt(signature = Base64.encode(cms))
    } catch (refusal: KalkanRefusal) {
        Attempt(problem = refusal.reason.problem())
    } finally {
        password.fill(ERASED)
    }

    private class Attempt(val signature: String? = null, val problem: KeyProblem? = null)

    private fun cancelled() = EdsRefusal(EdsProblem.Declined, Signer.CANCELLED)

    private companion object {
        const val ERASED = '\u0000'
    }
}

/** Причина Kalkan — неудачей с файлом, которую владелец видит в окне пароля. */
private fun KalkanReason.problem(): KeyProblem = when (this) {
    KalkanReason.WrongPassword -> KeyProblem.WrongPassword
    KalkanReason.Unreadable -> KeyProblem.Unreadable
    KalkanReason.NotForSigning -> KeyProblem.NotForSigning
    KalkanReason.Expired -> KeyProblem.Expired
}
