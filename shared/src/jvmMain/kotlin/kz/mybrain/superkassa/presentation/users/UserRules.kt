package kz.mybrain.superkassa.presentation.users

import kz.mybrain.superkassa.data.node.KkmUser
import kz.mybrain.superkassa.domain.signin.Pin
import kz.mybrain.superkassa.presentation.session.ADMIN_ROLE

/** Чем узел не устроит пин. */
enum class PinRefusal { TooShort, TooLong, Default }

/**
 * Правила заведения кассиров, проверенные до обращения к узлу.
 *
 * Все они повторяют проверки узла: он же откажет и по длине, и по
 * стандартному пину. Проверка на месте нужна не вместо узла, а чтобы
 * администратор увидел причину до того, как нажмёт «Завести».
 */
object UserRules {

    /** Короче четырёх цифр пин подобрать слишком легко. */
    const val MIN_PIN = Pin.MIN

    /** Длиннее восьми узел не принимает. */
    const val MAX_PIN = Pin.MAX

    /**
     * Пины, которые узел запрещает для управляющих команд.
     *
     * Заведённый с таким пином кассир смог бы войти, но не смог бы
     * ничего сделать — и решил бы, что касса сломана.
     */
    val FORBIDDEN_PINS: Set<String> = setOf("0000", "1111")

    /** Пин набирается цифрами и не длиннее допустимого: лишнее не вводится. */
    fun digitsOf(text: String): String = Pin.digitsOf(text)

    /** Что не так с пином. `null` — придраться не к чему либо ещё пусто. */
    fun checkPin(pin: String): PinRefusal? = when {
        pin.isEmpty() -> null
        pin.length < MIN_PIN -> PinRefusal.TooShort
        pin.length > MAX_PIN -> PinRefusal.TooLong
        pin in FORBIDDEN_PINS -> PinRefusal.Default
        else -> null
    }

    /**
     * Такой пин узел примет как **новый**.
     *
     * Стандартные пины сюда не проходят: узел не даст завести кассира
     * с ними и не даст ими управляющих команд.
     */
    fun pinAccepted(pin: String): Boolean =
        pin.isNotEmpty() && checkPin(pin) == null && pin.length >= MIN_PIN

    /**
     * Таким пином можно попробовать войти.
     *
     * Отличается от [pinAccepted] тем, что стандартный пин здесь допустим:
     * только что заведённая касса живёт как раз под ним, и запрет на вход
     * означал бы, что войти в неё нельзя вовсе — в том числе чтобы сменить
     * пин, как велит подсказка после заведения. Годен ли пин на деле,
     * решает узел.
     */
    fun pinEnterable(pin: String): Boolean = Pin.enterable(pin)

    /** Кассира можно завести: есть имя и годный пин. */
    fun canCreate(name: String, pin: String): Boolean = name.isNotBlank() && pinAccepted(pin)

    /**
     * Один и тот же кассир.
     *
     * Сравниваются опознаватели узла, а не имена: тёзок на кассе двое,
     * а пин у каждого свой. Безымянный опознаватель не считается своим —
     * иначе два неопознанных кассира оказались бы одним.
     */
    fun same(who: KkmUser?, user: KkmUser): Boolean =
        user.identifier.isNotEmpty() && who?.identifier == user.identifier

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
    fun lastAdmin(users: List<KkmUser>, user: KkmUser): Boolean =
        user.role == ADMIN_ROLE && users.count { it.role == ADMIN_ROLE } <= 1
}
