package kz.mybrain.superkassa.domain.users.model

import io.github.texport.superkassa.core.presentation.api.model.user.UserResponse
import io.github.texport.superkassa.core.presentation.api.model.user.UserRole
import kz.mybrain.superkassa.domain.signin.model.Pin

/** Чем касса не устроит пин. */
enum class PinRefusal { TooShort, TooLong }

/**
 * Правила заведения кассиров, проверенные до обращения к кассе.
 *
 * Все они повторяют проверки кассы: она откажет пину не той длины.
 * Особых пинов у кассы нет — пинов по умолчанию нет, и любой пин годной
 * длины обычный. Проверка на месте нужна не вместо кассы, а чтобы
 * администратор увидел причину до того, как нажмёт «Завести».
 */
object UserRules {

    /** Короче четырёх цифр пин подобрать слишком легко. */
    const val MIN_PIN = Pin.MIN

    /** Длиннее десяти касса не принимает. */
    const val MAX_PIN = Pin.MAX

    /** Пин набирается цифрами и не длиннее допустимого: лишнее не вводится. */
    fun digitsOf(text: String): String = Pin.digitsOf(text)

    /** Что не так с пином. `null` — придраться не к чему либо ещё пусто. */
    fun checkPin(pin: String): PinRefusal? = when {
        pin.isEmpty() -> null
        pin.length < MIN_PIN -> PinRefusal.TooShort
        pin.length > MAX_PIN -> PinRefusal.TooLong
        else -> null
    }

    /** Такой пин касса примет как **новый**: от четырёх до десяти цифр. */
    fun pinAccepted(pin: String): Boolean =
        pin.isNotEmpty() && checkPin(pin) == null && pin.length >= MIN_PIN

    /**
     * Таким пином можно попробовать войти.
     *
     * Годен ли пин для входа, решает касса, а приложение лишь не пускает
     * к ней заведомо неполный набор.
     */
    fun pinEnterable(pin: String): Boolean = Pin.enterable(pin)

    /** Кассира можно завести: есть имя и годный пин. */
    fun canCreate(name: String, pin: String): Boolean = name.isNotBlank() && pinAccepted(pin)

    /**
     * Один и тот же кассир.
     *
     * Сравниваются опознаватели кассы, а не имена: тёзок на кассе двое,
     * а пин у каждого свой. Безымянный опознаватель не считается своим —
     * иначе два неопознанных кассира оказались бы одним.
     */
    fun same(who: UserResponse?, user: UserResponse): Boolean =
        user.userId.isNotEmpty() && who?.userId == user.userId

    /**
     * Единственный администратор кассы.
     *
     * Удалить такого нельзя: касса осталась бы без права заводить кассиров
     * и менять настройки, а войти в неё было бы некем.
     *
     * Правило про администратора и только про него. Прежде оно запрещало
     * удалять последнего носителя **любой** роли, и администратор,
     * расставшийся с единственным кассиром, читал под его именем совет
     * завести второго кассира — чтобы потом удалить обоих. Касса без
     * кассиров — обычное дело: ровно такой она и выходит из мастера
     * подключения, где заводится один администратор.
     */
    fun lastAdmin(users: List<UserResponse>, user: UserResponse): Boolean =
        user.role == UserRole.ADMIN && users.count { it.role == UserRole.ADMIN } <= 1
}
