package kz.mybrain.superkassa.desktop.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kz.mybrain.superkassa.desktop.eds.EdsProblem
import kz.mybrain.superkassa.desktop.eds.EdsRefusal
import kz.mybrain.superkassa.desktop.eds.NcaLayer
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetClient
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetCompany
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetRefusal
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetRegister
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetUser
import kz.mybrain.superkassa.desktop.server.cabinet.edsChallenge
import kz.mybrain.superkassa.desktop.server.cabinet.edsLogin
import kz.mybrain.superkassa.desktop.server.cabinet.logout
import kz.mybrain.superkassa.desktop.server.cabinet.registers

/**
 * Работа в личном кабинете ОФД.
 *
 * Отдельный сеанс от кассового: в кабинет входит владелец по своей ЭЦП,
 * а за кассой стоит кассир со своим пином. Смешивать их в одном состоянии
 * значит однажды показать кассиру то, что видит владелец.
 *
 * Доступ живёт только в памяти приложения: на диск он не пишется. Ключ
 * ЭЦП сюда не попадает вовсе — его держит NCALayer.
 */
class CabinetSession(
    val client: CabinetClient = CabinetClient(),
    private val eds: NcaLayer = NcaLayer()
) {
    /** Выданный кабинетом доступ; `null` — владелец не входил. */
    var token: String? by mutableStateOf(null)
        private set

    var user: CabinetUser? by mutableStateOf(null)
        private set

    var company: CabinetCompany? by mutableStateOf(null)
        private set

    /** Кассы компании из кабинета. */
    var registers: List<CabinetRegister> by mutableStateOf(emptyList())
        private set

    /**
     * Что именно не получилось.
     *
     * Помеха не показывается на месте: её забирает каркас окна и выводит
     * тем же всплывающим сообщением, что и отказы кассы. Красная строка
     * посреди раздела оставалась висеть после исправления и терялась,
     * когда раздел прокручивали.
     */
    var problem: CabinetProblem? by mutableStateOf(null)
        private set

    /** Помеха показана — снять её. */
    fun clearProblem() {
        problem = null
    }

    /** Идёт обращение к кабинету или ожидание подписи. */
    var busy: Boolean by mutableStateOf(false)
        private set

    /** Вошёл ли владелец. */
    val open: Boolean get() = token != null

    /**
     * Вход по ЭЦП: кабинет выдаёт задачу, NCALayer её подписывает.
     *
     * Ждать подпись приходится долго — владелец выбирает сертификат
     * и вводит пароль в окне NCALayer, — поэтому раздел на это время
     * показывает ожидание, а не пустой экран.
     */
    suspend fun signIn(): Boolean = guard {
        val challenge = client.edsChallenge()
        val signature = eds.signCms(challenge.payload)
        val entered = client.edsLogin(challenge.challengeId, signature)
        token = entered.accessToken
        user = entered.user
        company = entered.company
        true
    } ?: false

    /** Выход: доступ отзывается и в кабинете, и здесь. */
    suspend fun signOut() {
        val current = token ?: return
        guard { client.logout(current) }
        token = null
        user = null
        company = null
        registers = emptyList()
    }

    /**
     * Подписывает содержимое ключом владельца.
     *
     * Обёртка вокруг NCALayer нужна, чтобы экраны не заводили своё
     * соединение с ним: подписывающий у сеанса один.
     */
    suspend fun sign(payload: String): String = eds.signCms(payload)

    /** Перечитывает кассы компании. */
    suspend fun refreshRegisters() {
        val current = token ?: return
        guard { registers = client.registers(current).items }
    }

    /**
     * Выполняет обращение к кабинету, отделяя отказ по существу
     * от недоступности кабинета и от отказа подписи.
     *
     * Истёкший доступ не отказ, а конец сеанса: владельца возвращает
     * ко входу, а не оставляет с пустым списком без объяснения.
     */
    suspend fun <T> guard(block: suspend () -> T): T? {
        problem = null
        busy = true
        return try {
            block()
        } catch (refusal: CabinetRefusal) {
            // Истёкшим доступ считается только когда он был: при входе
            // никакого доступа ещё нет, и 401 там означает отказ по
            // подписи — сертификат просрочен, корень не тот, подпись
            // не сходится. Прятать это за «войдите заново» значит
            // предлагать повторить то, что не сработает.
            if (refusal.httpStatus == UNAUTHORIZED && token != null) {
                token = null
                user = null
                company = null
                problem = CabinetProblem.SessionExpired
            } else {
                problem = CabinetProblem.Refused(refusal.code, refusal.text)
            }
            null
        } catch (refusal: EdsRefusal) {
            problem = when (refusal.problem) {
                EdsProblem.Unreachable -> CabinetProblem.NoNcaLayer
                EdsProblem.Declined -> CabinetProblem.SignDeclined(refusal.detail)
            }
            null
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            // Имя класса без текста ошибки не объясняет ничего: по
            // «IllegalArgumentException» неизвестно ни где, ни что.
            problem = CabinetProblem.Unreachable(
                listOfNotNull(failure::class.simpleName, failure.message)
                    .joinToString(" · ")
                    .take(MAX_REASON)
            )
            null
        } finally {
            busy = false
        }
    }

    /** Сообщение снимается, когда владелец начал новое действие. */
    fun forgetProblem() {
        problem = null
    }

    private companion object {
        const val UNAUTHORIZED = 401
        const val MAX_REASON = 300
    }
}

/** Почему действие в кабинете не удалось. */
sealed interface CabinetProblem {
    /** Кабинет ответил отказом по существу. */
    data class Refused(val code: String, val text: String) : CabinetProblem

    /** Кабинет не отвечает по заданному адресу. */
    data class Unreachable(val reason: String) : CabinetProblem

    /** NCALayer не запущен на этой машине. */
    data object NoNcaLayer : CabinetProblem

    /** Владелец не подписал: закрыл окно или ошибся паролем. */
    data class SignDeclined(val detail: String) : CabinetProblem

    /** Доступ истёк — нужно войти заново. */
    data object SessionExpired : CabinetProblem
}
