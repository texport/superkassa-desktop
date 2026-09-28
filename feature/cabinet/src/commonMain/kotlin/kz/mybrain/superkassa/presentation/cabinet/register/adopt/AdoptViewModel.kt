package kz.mybrain.superkassa.presentation.cabinet.register.adopt

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import io.github.texport.superkassa.core.presentation.api.model.reference.OfdEnvironmentResponse
import io.github.texport.superkassa.core.presentation.api.model.reference.TrilingualMessageResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.presentation.cabinet.CabinetViewModel
import kz.mybrain.superkassa.presentation.cabinet.value
import kz.mybrain.superkassa.presentation.common.model.shown
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Касса кабинета на этой машине.
 *
 * @property environments контуры БФД из справочника кассы.
 * @property stateNames названия состояний кассы на трёх языках — по коду.
 * @property issued выпущенные в этом окне токены — по кассе кабинета.
 * @property stranded у каких касс токен выпущен, а заведение сорвалось.
 * @property adopting заведение идёт.
 */
internal data class AdoptUiState(
    val environments: List<OfdEnvironmentResponse> = emptyList(),
    val stateNames: Map<String, TrilingualMessageResponse> = emptyMap(),
    val issued: Map<String, String> = emptyMap(),
    val stranded: Set<String> = emptySet(),
    val adopting: Boolean = false
)

/**
 * Заведение кассы кабинета на этой машине — одним действием владельца:
 * токен, заведение в кассе приложения, переход к кассе.
 *
 * Токен выпускается один раз: кабинет умеет только выпустить новый,
 * а показать действующий не умеет, и второй выпуск после сорванного
 * заведения убил бы и этот. Повтор шлёт кассе тот же токен.
 */
internal class AdoptViewModel(private val cabinet: CabinetViewModel) : ViewModel() {
    private val screen = MutableStateFlow(AdoptUiState())
    private val talk = cabinet.talk
    private val cases = cabinet.useCases

    val state: StateFlow<AdoptUiState> = screen.asStateFlow()

    init {
        viewModelScope.launch {
            cases.readEnvironments().value()?.let { found -> screen.update { it.copy(environments = found) } }
            cases.readKkmStates().value()?.let { found ->
                screen.update { it.copy(stateNames = found.associate { state -> state.code to state.name }) }
            }
        }
    }

    /**
     * Заводит кассу на этой машине.
     *
     * @param onDone касса заведена и выбрана на входе: окно закрывается.
     */
    fun adopt(register: CabinetRegister, environment: String, adminPin: String, onDone: () -> Unit) {
        if (screen.value.adopting) return
        screen.update { it.copy(adopting = true, stranded = it.stranded - register.id) }
        viewModelScope.launch {
            try {
                val token = tokenFor(register) ?: return@launch
                val what = textsOf(talk.language()).cabinet.machine.workHere
                val kkm = cases.enrollKkm(register, BFD_PROVIDER, environment, adminPin, token)
                    .okedExplained()
                    .shown(what, "adopt kkm", talk)
                if (kkm == null) {
                    screen.update { it.copy(stranded = it.stranded + register.id) }
                    return@launch
                }
                screen.update { it.copy(issued = it.issued - register.id) }
                cases.workOn(kkm)
                talk.done(textsOf(talk.language()).cabinet.machine.done)
                onDone()
            } finally {
                screen.update { it.copy(adopting = false) }
            }
        }
    }

    /** Переводит рабочее место на эту кассу: вход начинается заново — с её пина. */
    fun workOn(kkm: KkmResponse) = cases.workOn(kkm)

    private suspend fun tokenFor(register: CabinetRegister): String? =
        screen.value.issued[register.id] ?: issue(register)

    private suspend fun issue(register: CabinetRegister): String? {
        val issued = cabinet.work.run("reissue token") { cases.issueToken(register.id) }.value
        val token = issued?.token?.toString() ?: return null
        screen.update { it.copy(issued = it.issued + (register.id to token)) }
        return token
    }
}

/**
 * Отказ «ОКЭД обязателен» — словами о том, что делать.
 *
 * Касса сама говорит только «ОКЭД обязателен», и владелец не знал, где
 * его взять: ОКЭД кассы — вид деятельности компании из вкладки «Компания».
 */
private fun <T> Answer<T>.okedExplained(): Answer<T> {
    if (this !is Answer.Refused || code != OKED_REQUIRED) return this
    fun words(language: Language) = textsOf(language).cabinet.machine.okedMissing
    return copy(ru = words(Language.Ru), kk = words(Language.Kk), en = words(Language.En))
}

/** Код отказа кассы, заведённой без ОКЭДа. */
private const val OKED_REQUIRED = "OKED_REQUIRED"

/** Значение удавшегося ответа кассы; `null` — не удалось. */
private fun <T> Answer<T>.value(): T? = (this as? Answer.Done)?.value

/** Модель заведения касс кабинета на этой машине. */
@Composable
internal fun adoptViewModel(cabinet: CabinetViewModel): AdoptViewModel = viewModel { AdoptViewModel(cabinet) }
