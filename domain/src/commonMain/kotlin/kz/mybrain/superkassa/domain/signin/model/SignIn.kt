package kz.mybrain.superkassa.domain.signin.model

import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import io.github.texport.superkassa.core.presentation.api.model.user.UserResponse
import io.github.texport.superkassa.core.presentation.api.model.user.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kz.mybrain.superkassa.domain.signin.port.SignedKkm

/**
 * Кто сейчас работает за кассой: касса, кассир и его пин.
 *
 * Пин живёт только здесь и только в памяти: он даёт право на фискальные
 * команды, и записывать его на диск нельзя. По той же причине его нет
 * в [toString] — строка состояния попадает в журналы и отчёты проверок.
 *
 * @property kkm выбранная касса; остаётся и после ухода кассира.
 * @property cashier кто вошёл: имя и роль, как их знает касса.
 */
data class SignInState(
    val kkm: KkmResponse? = null,
    val cashier: UserResponse? = null,
    val pin: String = ""
) {
    /** Работа началась: касса выбрана, кассир назван и пин принят. */
    val signedIn: Boolean get() = kkm != null && cashier != null && pin.isNotEmpty()

    /** Права администратора: смена, кассиры, настройки. */
    val isAdmin: Boolean get() = cashier?.role == UserRole.ADMIN

    override fun toString(): String = "SignInState(kkm=${kkm?.kkmId}, cashier=${cashier?.userId}, signedIn=$signedIn)"
}

/**
 * За кассой всё тот же: та же касса и тот же кассир.
 *
 * Касса меняет своё состояние и под тем же кассиром — перечитана после
 * чека, включён режим программирования, — и набранное на экране остаётся.
 * Другой кассир или другая касса — экран начинает заново.
 */
fun SignInState.sameSeat(other: SignInState): Boolean =
    kkm?.kkmId == other.kkm?.kkmId && cashier?.userId == other.cashier?.userId && signedIn == other.signedIn

/**
 * Держатель входа: одно на всё приложение место, где записано, кто за кассой.
 *
 * Модели экранов следят за ним сценарием `ObserveSignIn` и меняют его
 * сценариями входа, выхода и смены кассы; сценарии других областей видят
 * его портом [SignedKkm] — только кассу и пин команды. Так вход, выход
 * и смена кассы выглядят одинаково из любого раздела.
 */
class SignIn : SignedKkm {
    private val current = MutableStateFlow(SignInState())

    val state: StateFlow<SignInState> = current.asStateFlow()

    /** Кассир вошёл: пин касса приняла и назвала, кто это. */
    fun enter(kkm: KkmResponse, cashier: UserResponse, pin: String) {
        current.value = SignInState(kkm, cashier, pin)
    }

    override fun seat(): Seat? {
        val now = current.value
        val kkm = now.kkm?.takeIf { now.signedIn } ?: return null
        return Seat(kkm.kkmId, now.pin)
    }

    /** Касса перечитана: её состояние сменилось, а кассир остался. */
    override fun refresh(kkm: KkmResponse) = current.update {
        if (it.kkm?.kkmId == kkm.kkmId) SignInState(kkm, it.cashier, it.pin) else it
    }

    /** Кассир сменил себе пин: работа продолжается с новым. */
    fun changePin(pin: String) = current.update { SignInState(it.kkm, it.cashier, pin) }

    /**
     * Кассир ушёл, касса осталась.
     *
     * Касса на рабочем месте одна и та же, а кассиры за смену меняются:
     * искать её в списке заново новому кассиру незачем.
     */
    fun signOut() = current.update { SignInState(kkm = it.kkm) }

    /** Уходит на выбор кассы: за этой машиной будет работать другая. */
    fun switchKkm() {
        current.value = SignInState()
    }
}
