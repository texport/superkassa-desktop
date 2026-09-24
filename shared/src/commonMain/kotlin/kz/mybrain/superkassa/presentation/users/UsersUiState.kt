package kz.mybrain.superkassa.presentation.users

import io.github.texport.superkassa.core.presentation.api.model.reference.TrilingualMessageResponse
import io.github.texport.superkassa.core.presentation.api.model.user.UserResponse
import io.github.texport.superkassa.core.presentation.api.model.user.UserRole
import kz.mybrain.superkassa.domain.users.model.UserRules

/**
 * Кассиры кассы, какими их видит администратор.
 *
 * @property users кассиры, как их отдала касса.
 * @property answered ответила ли касса на чтение: до ответа пустой список —
 *   не «кассиров нет», а «ещё не знаем».
 * @property unreadable касса список не отдала: пустая касса и касса, о кассирах
 *   которой не спросить, — разные беды.
 * @property me кто сейчас за кассой: ему одному меняют пин себе.
 * @property roleNames названия ролей со слов кассы; пусто — свои слова.
 * @property form набранное в заведении кассира.
 * @property pin смена пина, открытая сейчас; `null` — окна нет.
 * @property removing кассир, об удалении которого спрошено; `null` — вопроса нет.
 */
data class UsersUiState(
    val users: List<UserResponse> = emptyList(),
    val answered: Boolean = false,
    val unreadable: Boolean = false,
    val kkmChosen: Boolean = false,
    val me: UserResponse? = null,
    val roleNames: Map<String, TrilingualMessageResponse> = emptyMap(),
    val form: CashierForm = CashierForm(),
    val pin: PinChange? = null,
    val removing: UserResponse? = null
) {
    /** Этот кассир сейчас за кассой: пин себе и выход после удаления себя. */
    fun own(user: UserResponse): Boolean = UserRules.same(me, user)

    /** Удалить можно всех, кроме единственного администратора. */
    fun deletable(user: UserResponse): Boolean = !UserRules.lastAdmin(users, user)

    /** Кассира можно завести: касса выбрана, имя и годный пин набраны, прошлое заведение кончилось. */
    val canCreate: Boolean
        get() = kkmChosen && !form.busy && UserRules.canCreate(form.name, form.pin)
}

/**
 * Заведение кассира: имя, роль и пин.
 *
 * Живёт в модели, а не на экране: администратор, ушедший в журнал
 * за номером кассира, возвращается к набранному.
 */
data class CashierForm(
    val name: String = "",
    val role: UserRole = UserRole.CASHIER,
    val pin: String = "",
    val busy: Boolean = false
)

/**
 * Смена пина одному кассиру.
 *
 * Окно закрывается только по ответу кассы: на отказе кассир должен видеть,
 * что он ввёл и почему не вышло. Отказ стоит в окне, а не в строке
 * сообщений: строку внизу окна закрывает само окно.
 *
 * @property refusal почему касса не сменила пин, её словами.
 */
data class PinChange(
    val user: UserResponse,
    val own: Boolean,
    val pin: String = "",
    val busy: Boolean = false,
    val refusal: String? = null
) {
    /** Такой пин можно отправить кассе. */
    val ready: Boolean get() = !busy && UserRules.pinAccepted(pin)
}
