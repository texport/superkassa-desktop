package kz.mybrain.superkassa.data.eds

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kz.mybrain.superkassa.domain.cabinet.model.signature.SignDesk
import kz.mybrain.superkassa.domain.cabinet.model.signature.SignMethod
import kz.mybrain.superkassa.domain.cabinet.port.Signer
import kz.mybrain.superkassa.domain.cabinet.port.Signing

/**
 * Подписывающий, которого выбрал владелец, — из способов этой платформы.
 *
 * Вход, заявление и мастер зовут [sign] и не знают, чем подписывают:
 * подпись идёт способом, выбранным у кнопки входа. Выбор живёт, пока
 * живёт приложение, и не сохраняется: способ по умолчанию — первый.
 *
 * @param signers подписывающие по способам; порядок — порядок выбора.
 * @param desk стол подписи, общий у всех подписывающих этого набора.
 */
class ChosenSigner(private val signers: Map<SignMethod, Signer>, override val desk: SignDesk) : Signer, Signing {
    override val methods: List<SignMethod> = signers.keys.toList()
    private val chosen = MutableStateFlow(methods.first())

    override val method: StateFlow<SignMethod> = chosen.asStateFlow()

    override fun choose(method: SignMethod) {
        if (method in signers) chosen.value = method
    }

    override suspend fun sign(payload: String): String = signers.getValue(chosen.value).sign(payload)
}
