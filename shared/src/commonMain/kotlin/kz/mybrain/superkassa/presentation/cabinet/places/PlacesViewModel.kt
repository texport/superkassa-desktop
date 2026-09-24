package kz.mybrain.superkassa.presentation.cabinet.places

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.domain.cabinet.model.ChangeAddressResult
import kz.mybrain.superkassa.domain.cabinet.model.RegisterAddress
import kz.mybrain.superkassa.domain.cabinet.model.RetailPlace
import kz.mybrain.superkassa.presentation.cabinet.CabinetReply
import kz.mybrain.superkassa.presentation.cabinet.CabinetViewModel
import kz.mybrain.superkassa.presentation.cabinet.value
import kz.mybrain.superkassa.presentation.common.message.Message
import kz.mybrain.superkassa.presentation.map.MapPoint
import kz.mybrain.superkassa.presentation.theme.icon.Glyphs

/**
 * Выбор в колонке точек и отбор над ней.
 *
 * @property place выбранная точка; `null` — не выбрана.
 * @property register выбранная касса; при ней справа открыта её карточка.
 */
data class PlacesUiState(
    val place: String? = null,
    val register: String? = null,
    val sieve: PlaceSieve = PlaceSieve()
)

/**
 * Торговые точки: что выбрано в колонке и что с точками делают.
 *
 * Выбор живёт, пока открыто окно: владелец уходит на вкладку компании
 * и возвращается к той же кассе. Вышел владелец — выбор забывается вместе
 * с хозяйством.
 */
class PlacesViewModel(private val cabinet: CabinetViewModel) : ViewModel() {
    private val screen = MutableStateFlow(PlacesUiState())
    private val cases = cabinet.useCases

    val state: StateFlow<PlacesUiState> = screen.asStateFlow()

    init {
        viewModelScope.launch {
            cabinet.state.map { it.owner }.distinctUntilChanged().collect { screen.value = PlacesUiState() }
        }
    }

    fun selectPlace(id: String) = screen.update { it.copy(place = id, register = null) }

    fun selectRegister(id: String) = screen.update { it.copy(register = id) }

    fun sieve(sieve: PlaceSieve) = screen.update { it.copy(sieve = sieve) }

    /**
     * Заводит точку; заведённая сразу встаёт в список окна.
     *
     * Кабинет отдал уже заведённую точку с тем же адресом и местом — новой
     * нет, и окно остаётся открытым: владелец меняет место или передумывает.
     *
     * @param onAdded зовётся только по удаче: введённое при отказе остаётся на месте.
     */
    fun add(name: String, address: RegisterAddress, point: MapPoint, onAdded: (RetailPlace) -> Unit) {
        val (latitude, longitude) = point
        viewModelScope.launch {
            val known = cabinet.state.value.places
            val added = cabinet.work.run("add place") { cases.addPlace(name, address, latitude, longitude, known) }
                .value ?: return@launch
            if (added.existed) {
                val words = listOf(cabinet.texts.placeExists, added.place.name).joinToString(Glyphs.SEPARATOR)
                cabinet.talk.say("add place", Message.Refusal(words, PLACE_EXISTS))
                return@launch
            }
            cabinet.placeAdded(added.place)
            onAdded(added.place)
        }
    }

    fun rename(place: RetailPlace, name: String) {
        viewModelScope.launch {
            if (cabinet.work.run("rename place") { cases.renamePlace(place, name) } is CabinetReply.Done) {
                cabinet.reload()
            }
        }
    }

    /**
     * Переезд точки.
     *
     * Прямо адрес меняется только у точки без касс или с одними черновиками;
     * иначе кабинет называет кассы, которым нужна перерегистрация, — и это
     * не отказ кабинета, а ответ по существу, которого владелец ждал.
     *
     * @param onMoved зовётся, когда адрес сменился: форма переезда очищается.
     */
    fun move(place: RetailPlace, address: RegisterAddress, point: MapPoint, onMoved: () -> Unit) {
        val (latitude, longitude) = point
        viewModelScope.launch {
            val known = cabinet.state.value.registers
            val result = cabinet.work.run("move place") {
                cases.movePlace(place, address, latitude, longitude, known)
            }.value ?: return@launch
            if (!result.updated) {
                cabinet.talk.say("move place", Message.Refusal(blockedWords(result), REREGISTRATION))
                return@launch
            }
            cabinet.talk.done(cabinet.texts.addressChanged)
            onMoved()
            cabinet.reload()
        }
    }

    /** Удаляет точку без касс. */
    fun remove(place: RetailPlace) {
        viewModelScope.launch {
            if (cabinet.work.run("remove place") { cases.removePlace(place) } is CabinetReply.Done) {
                screen.update { if (it.place == place.id) it.copy(place = null, register = null) else it }
                cabinet.reload()
            }
        }
    }

    /** Почему адрес не сменился: причина и кассы, которые мешают. */
    private fun blockedWords(result: ChangeAddressResult): String {
        val blocked = result.blockingCashRegisters.map { it.title() }.filter { it.isNotBlank() }
        return listOf(cabinet.texts.addressNeedsReregistration, blocked.joinToString(", "))
            .filter { it.isNotBlank() }
            .joinToString(Glyphs.SEPARATOR)
    }

    private companion object {
        /** Код, которым приложение называет несостоявшийся переезд. */
        const val REREGISTRATION = "REREGISTRATION_REQUIRED"

        /** Код, которым приложение называет точку, заведённую раньше по тому же адресу и месту. */
        const val PLACE_EXISTS = "RETAIL_PLACE_EXISTS"
    }
}

/** Модель точек окна. */
@Composable
fun placesViewModel(cabinet: CabinetViewModel): PlacesViewModel = viewModel { PlacesViewModel(cabinet) }
