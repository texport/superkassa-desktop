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
import kz.mybrain.superkassa.desktop.server.cabinet.PositionSource
import kz.mybrain.superkassa.desktop.server.cabinet.RetailPlace
import kz.mybrain.superkassa.desktop.server.cabinet.allRegisters
import kz.mybrain.superkassa.desktop.server.cabinet.allRetailPlaces
import kz.mybrain.superkassa.desktop.server.cabinet.cashRegisterMap
import kz.mybrain.superkassa.desktop.server.cabinet.edsChallenge
import kz.mybrain.superkassa.desktop.server.cabinet.edsLogin
import kz.mybrain.superkassa.desktop.server.cabinet.logout

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
        placesTotal = 0
        blockedRegisters = null
    }

    /**
     * Подписывает содержимое ключом владельца.
     *
     * Обёртка вокруг NCALayer нужна, чтобы экраны не заводили своё
     * соединение с ним: подписывающий у сеанса один.
     */
    suspend fun sign(payload: String): String = eds.signCms(payload)

    /**
     * Сколько точек у компании по словам кабинета.
     *
     * Пока список читается, прочитано меньше: колонка говорит «Показано
     * 50 из 2000», а не выдаёт первую страницу за всё хозяйство.
     */
    var placesTotal: Int by mutableStateOf(0)
        private set

    /**
     * Перечитывает кассы компании — все, а не первую страницу.
     *
     * Список выкладывается страницами по мере чтения: у сети их сорок,
     * и ждать последнюю, глядя в пустую колонку, владельцу незачем.
     * Выкладывается, пока прочитанное длиннее показанного: перечитывание
     * уже прочитанного списка иначе сбрасывало его до первой полусотни
     * и набирало заново на глазах у владельца. Укоротившийся список
     * принимается в конце чтения — целиком, каким его отдал кабинет.
     * Перечитывать весь список ради одной изменившейся кассы тоже
     * незачем — для этого есть [registerChanged].
     */
    suspend fun refreshRegisters() {
        val current = token ?: return
        val all = guard {
            client.allRegisters(current) { part, _ -> if (part.size >= registers.size) registers = part }
        }
        if (all != null) registers = all
        onRegisterNames?.invoke(registers)
    }

    /**
     * Какие кассы кабинет считает заблокированными.
     *
     * `null` — не спрашивали или ответа не было. Отличать это от пустого
     * набора обязательно: непрочитанное, выданное за «заблокированных
     * нет», оставило бы владельца с пустым списком под нажатой плашкой.
     */
    var blockedRegisters: Set<String>? by mutableStateOf(null)
        private set

    /**
     * Перечитывает блокировки касс.
     *
     * Отдельным обращением, потому что в списке касс блокировки нет
     * вовсе: кабинет отдаёт её сводкой по всем кассам компании — той же,
     * что рисует карту аналитики. Обращение одно на весь список, и стоит
     * оно меньше, чем одна из сотни страниц самого списка.
     *
     * Молча: блокировок владелец не запрашивал, и отказ по ним не должен
     * закрывать собой список точек, ради которого он сюда пришёл.
     */
    suspend fun refreshBlocked() {
        val current = token ?: return
        val view = quiet("cabinet blocked registers") {
            client.cashRegisterMap(current, PositionSource.RetailPlaceAddress)
        } ?: return
        blockedRegisters = (view.placed + view.withoutPosition)
            .filter { it.blocked }
            .map { it.cashRegisterId }
            .toSet()
    }

    /**
     * Заменяет в списке одну перечитанную кассу.
     *
     * Карточка кассы перечитывает себя сама — и при открытии, и каждые
     * несколько секунд, пока ИСНА рассматривает заявление. Прежде вместе
     * с ней перечитывался весь список касс компании: у сети это сорок
     * запросов на каждый круг опроса ради строки, которая уже прочитана.
     */
    fun registerChanged(register: CabinetRegister) {
        registers = registers.map { if (it.id == register.id) register else it }
    }

    /**
     * Перечитывает торговые точки компании — все, а не первую страницу.
     *
     * Страницами выкладывается и здесь, и по тому же правилу, что у касс:
     * колонка не пустеет от перечитывания.
     *
     * @return удалось ли прочитать: по одному опустевшему списку колонка
     *   не отличает хозяйство без точек от молчащего кабинета, и владельцу
     *   с тысячей точек предлагалось завести первую.
     */
    suspend fun refreshPlaces(): Boolean {
        val current = token ?: return false
        val all = guard {
            client.allRetailPlaces(current) { part, total ->
                placesTotal = total.toInt()
                if (part.size >= places.size) places = part
            }
        }
        if (all != null) places = all
        return all != null
    }

    /**
     * Ставит в список только что заведённую точку.
     *
     * Кабинет отдаёт её в ответе на заведение, но в списке она появляется
     * не сразу: запрошенный через десятую долю секунды список приходил ещё
     * без неё, и владелец, заведя точку, не находил её на экране —
     * помогал лишь переход по разделам. Показываем сказанное кабинетом
     * о самой точке, а перечитанный список потом её подтвердит.
     */
    fun placeAdded(place: RetailPlace) {
        if (places.none { it.id == place.id }) {
            places = places + place
        }
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
