package kz.mybrain.superkassa.desktop.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kz.mybrain.superkassa.desktop.app.log.AppLog
import kz.mybrain.superkassa.desktop.app.log.LogSource
import kz.mybrain.superkassa.desktop.eds.EdsRefusal
import kz.mybrain.superkassa.desktop.eds.NcaLayer
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetClient
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetCompany
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetRefusal
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetRegister
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetUser
import kz.mybrain.superkassa.desktop.server.cabinet.RetailPlace
import kz.mybrain.superkassa.desktop.server.cabinet.edsChallenge
import kz.mybrain.superkassa.desktop.server.cabinet.edsLogin
import kz.mybrain.superkassa.desktop.server.cabinet.logout
import kz.mybrain.superkassa.desktop.server.cabinet.registers
import kz.mybrain.superkassa.desktop.server.cabinet.retailPlaces

/**
 * Работа в личном кабинете ОФД.
 *
 * Отдельный сеанс от кассового: в кабинет входит владелец по своей ЭЦП,
 * а за кассой стоит кассир со своим пином. Смешивать их в одном состоянии
 * значит однажды показать кассиру то, что видит владелец.
 *
 * Сам сеанс держит только то, что видно с экрана кабинета. Доступ владельца
 * живёт в [CabinetAccess], разбор помех — в [CabinetProblem], вход без ЭЦП —
 * Ключ ЭЦП сюда не попадает вовсе — его держит NCALayer.
 */
class CabinetSession(
    val client: CabinetClient = CabinetClient(),
    private val eds: NcaLayer = NcaLayer()
) {
    internal val access = CabinetAccess()

    /**
     * Куда отдать названия касс, прочитанные в кабинете.
     *
     * Названия кассам даёт владелец в кабинете, а нужны они кассиру
     * на входе — до того, как кабинет вообще открыт. Поэтому прочитанное
     * один раз уходит узлу. Кому именно отдавать, сеанс кабинета не знает:
     * он знает только, что названия появились.
     */
    var onRegisterNames: (suspend (List<CabinetRegister>) -> Unit)? = null

    /** Выданный кабинетом доступ; `null` — владелец не входил. */
    val token: String? get() = access.token

    val user: CabinetUser? get() = access.user

    val company: CabinetCompany? get() = access.company

    /** Кассы компании из кабинета. */
    var registers: List<CabinetRegister> by mutableStateOf(emptyList())
        private set

    /**
     * Торговые точки компании.
     *
     * Список один на сеанс: заявление о перерегистрации выбирает точку
     * из него же, и своей копии не держит. Со своей копией только что
     * созданная точка в заявлении не появлялась до повторного входа.
     */
    var places: List<RetailPlace> by mutableStateOf(emptyList())
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

    /**
     * Сколько раз кабинет ответил по существу.
     *
     * Счётчик, а не флаг: каркасу нужно знать не «отвечает ли кабинет
     * сейчас», а «ответил ли он после того, как я показал отказ». Отказ
     * висит на экране, пока его не закроют, и без этой отметки владелец
     * читал «Кабинет не отвечает» над четвёртым успешно пройденным шагом
     * мастера.
     */
    var answered: Int by mutableStateOf(0)
        private set

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
        access.enter(client.edsLogin(challenge.challengeId, eds.signCms(challenge.payload)))
        true
    } ?: false

    /** Выход: доступ отзывается и в кабинете, и здесь. */
    suspend fun signOut() {
        val current = token ?: return
        guard { client.logout(current) }
        access.forget()
        registers = emptyList()
        places = emptyList()
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
        onRegisterNames?.invoke(registers)
    }

    /** Перечитывает торговые точки компании. */
    suspend fun refreshPlaces() {
        val current = token ?: return
        guard { places = client.retailPlaces(current).items }
    }

    /**
     * Выполняет обращение к кабинету, отделяя отказ по существу
     * от недоступности кабинета и от отказа подписи.
     *
     * Истёкший доступ не отказ, а конец сеанса: владельца возвращает
     * ко входу, а не оставляет с пустым списком без объяснения.
     * Кто есть кто среди исключений — в [CabinetProblem].
     */
    suspend fun <T> guard(block: suspend () -> T): T? {
        problem = null
        busy = true
        return try {
            block().also { answered += 1 }
        } catch (refusal: CabinetRefusal) {
            problem = if (refusal.endsSession(entered = token != null)) endSession() else refusal.asProblem()
            null
        } catch (refusal: EdsRefusal) {
            problem = refusal.asProblem()
            null
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            problem = failure.asCabinetProblem()
            null
        } finally {
            busy = false
        }
    }

    /**
     * Выполняет обращение, о котором владельцу знать незачем.
     *
     * Отличается от [guard] молчанием: справочное наименование владелец
     * не запрашивал, и отказ по нему не должен закрывать собой то, что
     * он делает сейчас. Отказ уходит в журнал.
     */
    suspend fun <T> quiet(what: String, block: suspend () -> T): T? = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        AppLog.warn(LogSource.Cabinet, "$what: ${failure::class.simpleName}")
        null
    }

    /** Доступ истёк: сеанс кончился, и владельца возвращает ко входу. */
    private fun endSession(): CabinetProblem {
        access.forget()
        return CabinetProblem.SessionExpired
    }
}
