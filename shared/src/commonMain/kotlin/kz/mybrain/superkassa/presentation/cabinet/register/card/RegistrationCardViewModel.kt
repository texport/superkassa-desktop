package kz.mybrain.superkassa.presentation.cabinet.register.card

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.domain.cabinet.model.RegistrationCardVersion
import kz.mybrain.superkassa.domain.cabinet.model.documents.RegistrationCard
import kz.mybrain.superkassa.presentation.cabinet.CabinetViewModel
import kz.mybrain.superkassa.presentation.cabinet.value

/**
 * Регистрационная карта выбранной кассы и её версии.
 *
 * @property available кабинет объявил, что карта у кассы есть: объявил — читается заново.
 * @property card действующая карта; `null` — кабинет её не выдал или ещё не ответил.
 * @property cardAsked кабинет ответил о действующей карте.
 * @property versions версии карты; `null` — кабинет ещё не ответил.
 * @property opened раскрытая версия.
 * @property versionCards карты раскрытых версий — по номеру; ключ без
 *   значения — кабинет ответил, а карты нет.
 */
data class RegistrationCardUiState(
    val registerId: String? = null,
    val available: Boolean = false,
    val card: RegistrationCard? = null,
    val cardAsked: Boolean = false,
    val versions: List<RegistrationCardVersion>? = null,
    val opened: Int? = null,
    val versionCards: Map<Int, RegistrationCard?> = emptyMap()
)

/**
 * Регистрационная карта: что записано в учёте КГД и как это менялось.
 *
 * Карта той или иной версии — что было записано: адрес, точка, модель —
 * читается по раскрытию строки: спрашивать кабинет за все версии разом
 * ради одной незачем.
 */
class RegistrationCardViewModel(private val cabinet: CabinetViewModel) : ViewModel() {
    private val screen = MutableStateFlow(RegistrationCardUiState())
    private val cases = cabinet.useCases

    val state: StateFlow<RegistrationCardUiState> = screen.asStateFlow()

    /** Касса на экране: другая — карта читается заново. */
    fun show(register: CabinetRegister) {
        val now = screen.value
        if (now.registerId == register.id && now.available == register.registrationCardAvailable) return
        screen.value = RegistrationCardUiState(registerId = register.id, available = register.registrationCardAvailable)
        viewModelScope.launch {
            val card = if (register.registrationCardAvailable) {
                cabinet.work.run("read registration card") { cases.readRegistrationCard(register.id) }.value
            } else {
                null
            }
            val versions = if (register.registrationNumber.isNullOrBlank()) {
                emptyList()
            } else {
                cabinet.work.run("read card versions") { cases.readCardVersions(register.id) }.value.orEmpty()
            }
            update(register.id) { it.copy(card = card, cardAsked = true, versions = versions) }
        }
    }

    /** Раскрывает версию или сворачивает раскрытую. */
    fun open(register: CabinetRegister, version: Int) {
        val now = screen.value
        if (now.opened == version) return update(register.id) { it.copy(opened = null) }
        update(register.id) { it.copy(opened = version) }
        if (version in now.versionCards) return
        viewModelScope.launch {
            val card = cabinet.work.run("read card version") { cases.readCardVersion(register.id, version) }.value
            update(register.id) { it.copy(versionCards = it.versionCards + (version to card)) }
        }
    }

    /** Сохраняет в файл действующую карту — или версию, если она названа. */
    fun save(register: CabinetRegister, version: Int? = null) {
        viewModelScope.launch {
            val saved = cabinet.work.run("save card pdf") { cases.saveCardPdf(register, version) }.value
                ?: return@launch
            cabinet.talk.done("${cabinet.texts.savePdf}: $saved")
        }
    }

    /** Прочитанное кладётся, только если касса та же: ответ по прежней кассе запоздал. */
    private fun update(registerId: String, change: (RegistrationCardUiState) -> RegistrationCardUiState) =
        screen.update { if (it.registerId == registerId) change(it) else it }
}

/** Модель регистрационной карты окна: одна, и следует за выбранной кассой. */
@Composable
fun registrationCardViewModel(cabinet: CabinetViewModel): RegistrationCardViewModel =
    viewModel { RegistrationCardViewModel(cabinet) }
