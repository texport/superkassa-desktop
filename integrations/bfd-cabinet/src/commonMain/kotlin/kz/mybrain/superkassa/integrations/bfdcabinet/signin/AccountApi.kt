package kz.mybrain.superkassa.integrations.bfdcabinet.signin

import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import kotlinx.coroutines.flow.StateFlow
import kz.mybrain.superkassa.integrations.bfdcabinet.CabinetFailure
import kz.mybrain.superkassa.integrations.bfdcabinet.CabinetSigner
import kz.mybrain.superkassa.integrations.bfdcabinet.transport.CabinetCall
import kz.mybrain.superkassa.integrations.bfdcabinet.transport.CabinetHttp
import kz.mybrain.superkassa.integrations.bfdcabinet.transport.decoded
import kz.mybrain.superkassa.integrations.bfdcabinet.transport.encoded

/**
 * Вход владельца в кабинет и выход из него.
 *
 * При входе доступа ещё нет, и 401 там означает отказ по подписи —
 * сертификат просрочен, корень не тот, — а не конец сеанса: прятать это
 * за «войдите заново» значит предлагать повторить то, что не сработает.
 */
class AccountApi internal constructor(
    private val http: CabinetHttp,
    private val access: CabinetAccess,
    private val signer: CabinetSigner,
    private val development: Boolean
) {
    /** Кто вошёл; `null` — никто не входил, вышел или доступ истёк. */
    val owner: StateFlow<CabinetOwner?> get() = access.owner

    /** Адрес кабинета: владелец видит, куда входит. */
    val address: String get() = http.address

    /**
     * Вход: по ЭЦП, а в режиме разработки — по личности из настроек.
     *
     * По ЭЦП кабинет выдаёт задачу, подписывающий её подписывает, кабинет
     * выдаёт доступ. Вызов длится, пока владелец выбирает сертификат.
     *
     * @return вошедший.
     * @throws CabinetFailure кабинет отказал или не ответил.
     */
    suspend fun signIn(): CabinetOwner = if (development) enterAsDeveloper() else enterBySignature()

    /** Выход: доступ отзывается и в кабинете, и здесь — даже если кабинет не ответил. */
    suspend fun signOut() {
        val current = access.current ?: return
        try {
            http.send(CabinetCall(HttpMethod.Post, "/api/auth/logout", token = current))
        } finally {
            access.forget()
        }
    }

    /** Кто вошёл по действующему доступу — со сроком доступа. */
    suspend fun me(): CabinetMe = access.call { token -> me(token) }

    private suspend fun enterBySignature(): CabinetOwner {
        val challenge = decoded<EdsChallenge>(CHALLENGE, answer(CabinetCall(HttpMethod.Post, CHALLENGE)))
        val signed = EdsLoginRequest(challenge.challengeId, signer.sign(challenge.payload))
        val login = decoded<CabinetLogin>(LOGIN, answer(CabinetCall(HttpMethod.Post, LOGIN, encoded(signed))))
        return CabinetOwner(login.user, login.company).also { access.keep(login.accessToken, it) }
    }

    /** Режим разработки: личность уходит заголовками, доступ заменён отметкой. */
    private suspend fun enterAsDeveloper(): CabinetOwner {
        val me = me(token = null)
        return CabinetOwner(me.user, me.company).also { access.keep(DEVELOPMENT, it) }
    }

    private suspend fun me(token: String?): CabinetMe =
        decoded(ME, answer(CabinetCall(HttpMethod.Get, ME, token = token)))

    private suspend fun answer(call: CabinetCall): String = http.send(call).bodyAsText()

    private companion object {
        const val CHALLENGE = "/api/auth/eds/challenge"
        const val LOGIN = "/api/auth/eds"
        const val ME = "/api/auth/me"

        /** Отметка доступа в режиме разработки: кабинету она не передаётся. */
        const val DEVELOPMENT = "development"
    }
}
