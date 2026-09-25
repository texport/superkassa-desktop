package kz.mybrain.superkassa.presentation.cabinet.address

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.designsystem.theme.motion.Durations
import kz.mybrain.superkassa.domain.cabinet.model.AddressSuggestion
import kz.mybrain.superkassa.domain.cabinet.model.RegisterAddress
import kz.mybrain.superkassa.presentation.cabinet.CabinetViewModel
import kz.mybrain.superkassa.presentation.cabinet.component.askableQuery
import kz.mybrain.superkassa.presentation.cabinet.value

/**
 * Подбор адреса в государственном регистре по шагам.
 *
 * Свободного поиска по адресу целиком регистр не даёт: адрес собирается
 * из региона, населённых пунктов, улицы и дома. После каждого пункта
 * регистр спрашивается, есть ли вложенные, и только затем подбор переходит
 * к улице. Выбор дома завершает подбор: регистр подтверждает адрес по коду
 * РКА, и он уходит наружу целиком.
 *
 * Поиск идёт за набором, а не по кнопке: регистр отвечает быстро, а кнопка
 * у каждого шага читалась как ещё одно действие.
 */
internal class AddressSearchViewModel(private val cabinet: CabinetViewModel) : ViewModel() {
    private val screen = MutableStateFlow(AddressPath())
    private val cases = cabinet.useCases
    private var looking: Job? = null

    val state: StateFlow<AddressPath> = screen.asStateFlow()

    /** Начать подбор заново: сменилась точка, чей адрес подбирается. */
    fun reset() {
        looking?.cancel()
        screen.value = AddressPath()
        type("")
    }

    /**
     * Набранное в поле шага. Какой запрос регистр принимает, решает
     * [askableQuery]; на отвергнутом запросе список остаётся с прошлого.
     */
    fun type(query: String) {
        screen.update { it.copy(query = query) }
        val needle = query.trim()
        if (!askableQuery(needle)) return
        looking?.cancel()
        looking = viewModelScope.launch {
            if (needle.isNotEmpty()) delay(Durations.afterTyping)
            val path = screen.value
            val parent = path.chosen.lastOrNull()?.id ?: 0L
            val found = cabinet.work.run("look up address") { cases.lookUpAddress(path.level, parent, needle) }.value
            screen.update { it.copy(found = found.orEmpty(), searched = true) }
        }
    }

    /**
     * Выбор подсказки. Дом подтверждается регистром и уходит в [onAddress];
     * за пунктом регистр спрашивается о вложенных.
     */
    fun choose(suggestion: AddressSuggestion, onAddress: (RegisterAddress) -> Unit) {
        val next = screen.value.choose(suggestion)
        screen.value = next
        val building = next.building
        viewModelScope.launch {
            if (building != null) {
                resolve(building, onAddress)
            } else {
                probe(next)
                type("")
            }
        }
    }

    /** Правка выбранного уровня снимает его и всё, что ниже. */
    fun dropFrom(at: Int) {
        screen.update { it.dropFrom(at) }
        type("")
    }

    private suspend fun resolve(building: AddressSuggestion, onAddress: (RegisterAddress) -> Unit) {
        val rka = building.rka ?: return
        val address = cabinet.work.run("resolve address") { cases.resolveAddress(rka) }.value ?: return
        onAddress(address)
        screen.value = AddressPath()
    }

    /** У пункта могут быть вложенные пункты: до ответа следующий шаг не показывается. */
    private suspend fun probe(path: AddressPath) {
        val last = path.chosen.lastOrNull()
        if (last?.level != LEVEL_LOCALITY) return
        val nested = cabinet.work.run("probe nested localities") { cases.hasNested(last.id) }.value == true
        screen.update { it.copy(nestedUnderLast = nested, probed = true) }
    }
}

/**
 * Модель подбора адреса одного места: у формы новой точки своя, у переезда
 * точки своя — выбранное для одной не должно показываться у другой.
 */
@Composable
internal fun addressSearchViewModel(cabinet: CabinetViewModel, place: String): AddressSearchViewModel =
    viewModel(key = "address-search:$place") { AddressSearchViewModel(cabinet) }
