package kz.mybrain.superkassa.domain.cabinet.port

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kz.mybrain.superkassa.domain.cabinet.model.signature.SignDesk
import kz.mybrain.superkassa.domain.cabinet.model.signature.SignMethod

/**
 * Чем подписывать на этой платформе и что подписывающий спрашивает у владельца.
 *
 * Рядом с [Signer], а не в нём: вход, заявление и мастер просят только
 * подпись и не знают ни о QR, ни о паролях. Способ выбирает владелец
 * у кнопки входа, а подписывающий просит у него недостающее через [desk].
 */
interface Signing {

    /** Способы этой платформы; первый — по умолчанию. Один способ — выбирать нечего. */
    val methods: List<SignMethod>

    /** Выбранный способ. */
    val method: StateFlow<SignMethod>

    /** Выбирает способ из [methods]; чужой способ не выбирается. */
    fun choose(method: SignMethod)

    /** Стол подписи: что показать владельцу посреди подписи. */
    val desk: SignDesk

    /** Один способ и ничего не спрашивающий подписывающий — NCALayer на компьютере. */
    class Only(single: SignMethod) : Signing {
        override val methods: List<SignMethod> = listOf(single)
        override val method: StateFlow<SignMethod> = MutableStateFlow(single).asStateFlow()
        override val desk: SignDesk = SignDesk()

        override fun choose(method: SignMethod) = Unit
    }

    /** Подпись без выбора. */
    companion object {
        /** Только NCALayer: настольная касса — окно подписи он показывает сам. */
        val NcaLayerOnly: Signing = Only(SignMethod.NcaLayer)
    }
}
