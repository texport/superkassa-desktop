package kz.mybrain.superkassa.presentation.cabinet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.domain.cabinet.model.CabinetOwner
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.domain.cabinet.model.RetailPlace
import kz.mybrain.superkassa.presentation.cabinet.signing.CabinetSigning
import kz.mybrain.superkassa.presentation.common.model.Talk
import kz.mybrain.superkassa.strings.api.textsOf
import kotlin.time.TimeSource

/**
 * Кабинет окна: вход владельца по ЭЦП, подпись и хозяйство его компании.
 *
 * Модель живёт, пока открыто окно: владелец уходит на вход кассы и обратно,
 * а вошедший, списки и выбранное остаются. Вышел владелец или истёк доступ —
 * списки забываются вместе с ним: чужое хозяйство на экране после выхода
 * хуже пустого экрана.
 *
 * Модели разделов кабинета берут отсюда [work] — одну занятость на окно, —
 * [useCases] — сценарии кабинета — и [talk] — строку сообщений окна,
 * и сюда же сообщают о своих переменах: заведённой точке, перечитанной кассе.
 */
class CabinetViewModel(val useCases: CabinetCases, val talk: Talk) : ViewModel() {
    val work = CabinetWork(talk)
    private val screen = MutableStateFlow(CabinetUiState(address = useCases.address()))
    private val lists = CabinetLists(talk, useCases, work, screen)
    private var signing: Job? = null

    /** Читать ли хозяйство сразу после входа: мастер подключения его не читает. */
    private var listsOnEnter = true

    val state: StateFlow<CabinetUiState> = screen.asStateFlow()

    /** Чем владелец подписывает и что подписывающий просит у него сейчас. */
    val signature = CabinetSigning(useCases.signing)

    init {
        viewModelScope.launch { useCases.owner().collect(::follow) }
        viewModelScope.launch { work.running.collect { count -> screen.update { it.copy(running = count) } } }
    }

    /**
     * Вход по ЭЦП; вошёл — хозяйство читается сразу (см. [follow]).
     *
     * Ожидание подписи объявляется до начала работы и снимается в любом
     * исходе: иначе отмена или отказ оставили бы на экране отсчёт, за которым
     * уже никто не ждёт.
     */
    /**
     * @param lists читать ли хозяйство сети сразу после входа. Мастер
     *   подключения входит ради одной кассы и не читает: обход всех касс
     *   и точек сети — сотни обращений к кабинету подряд. Раздел кабинета,
     *   открытый потом, читает хозяйство сам.
     */
    fun signIn(lists: Boolean = true) {
        if (signing?.isActive == true) return
        listsOnEnter = lists
        screen.update { it.copy(signingSince = TimeSource.Monotonic.markNow()) }
        signing = viewModelScope.launch {
            try {
                work.run("sign in") { useCases.signIn() }
            } finally {
                screen.update { it.copy(signingSince = null) }
            }
        }
    }

    /** Владелец передумал ждать подпись. */
    fun cancelSignIn() {
        signing?.cancel()
    }

    fun signOut() {
        viewModelScope.launch { work.run("sign out") { useCases.signOut() } }
    }

    /** Перечитывает точки, кассы и блокировки. */
    fun reload() {
        viewModelScope.launch { lists.readAll() }
    }

    /** Перечитывает точки; `false` — кабинет их не отдал. */
    suspend fun readPlaces(): Boolean = lists.readPlaces()

    /**
     * Ставит в список только что заведённую точку.
     *
     * Кабинет отдаёт её в ответе на заведение, но в списке она появляется
     * не сразу: запрошенный через десятую долю секунды список приходил ещё
     * без неё, и владелец не находил только что заведённую точку.
     */
    fun placeAdded(place: RetailPlace) = lists.placeAdded(place)

    /**
     * Заменяет в списке одну перечитанную кассу.
     *
     * Карточка кассы перечитывает себя сама — и при открытии, и пока ИСНА
     * рассматривает заявление, — и перечитывать ради неё сорок страниц
     * списка сети незачем.
     */
    fun registerChanged(register: CabinetRegister) = lists.registerChanged(register)

    /** Открывает документы кассы поверх кабинета: выбранная касса остаётся выбранной. */
    fun openDocuments(register: CabinetRegister) = screen.update { it.copy(documentsOf = register) }

    fun closeDocuments() = screen.update { it.copy(documentsOf = null) }

    /**
     * Вошёл владелец — хозяйство его компании читается сразу; вышел или
     * истёк доступ — списки забываются вместе с ним.
     */
    private fun follow(owner: CabinetOwner?) {
        val before = screen.value.owner
        if (owner == null) {
            screen.update { CabinetUiState(address = it.address, running = it.running, signingSince = it.signingSince) }
            return
        }
        screen.update { it.copy(owner = owner) }
        if (before == null && listsOnEnter) viewModelScope.launch { lists.readAll() }
    }

    /** Слова кабинета на языке кассира сейчас. */
    internal val texts get() = textsOf(talk.language()).cabinet
}
