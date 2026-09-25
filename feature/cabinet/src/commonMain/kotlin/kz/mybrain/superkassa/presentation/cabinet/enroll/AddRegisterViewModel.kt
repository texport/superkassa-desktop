package kz.mybrain.superkassa.presentation.cabinet.enroll

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.domain.cabinet.model.RegisterCreate
import kz.mybrain.superkassa.presentation.cabinet.CabinetViewModel
import kz.mybrain.superkassa.presentation.cabinet.value
import kz.mybrain.superkassa.presentation.common.model.latest
import kz.mybrain.superkassa.presentation.common.model.shown
import kotlin.time.Duration.Companion.milliseconds

/**
 * Заведение кассы в кабинете: справочник моделей, свой заводской номер
 * и сама касса.
 *
 * Заведённая касса — ещё не поставленная на учёт: она получает состояние
 * «черновик» и ждёт заявления в ИСНА. Модель и точка выбираются из
 * справочников кабинета: произвольные значения ИСНА не примет.
 */
internal class AddRegisterViewModel(private val cabinet: CabinetViewModel) : ViewModel() {
    private val screen = MutableStateFlow(AddRegisterUiState())
    private val searching = latest()

    /** Справочник моделей и найденные кабинетом точки. */
    val state: StateFlow<AddRegisterUiState> = screen.asStateFlow()

    /**
     * Форму открыли: точки спрашиваются у кабинета заново, справочник
     * моделей — один раз, пока он не прочитан.
     *
     * Точки спрашиваются каждый раз: их заводят и в другом окне, и в
     * кабинете с другой машины, а касса ставится только на точку из списка.
     * Спрашивается одна страница — поиском кабинета, а не обходом всех
     * страниц сети: на пути мастера это были десятки обращений подряд.
     */
    fun open() {
        findPlaces("")
        if (screen.value.models.isNotEmpty()) return
        viewModelScope.launch {
            val models = cabinet.work.run("read kkm models") { cabinet.useCases.readModels() }.value
            if (models != null) screen.update { it.copy(models = models) }
        }
    }

    /** Владелец набирает точку: кабинет ищет её сам, последний набор отменяет прежний. */
    fun findPlaces(text: String) {
        searching.restart {
            // Набор ещё идёт: кабинет спрашивается, когда владелец остановился.
            if (text.isNotEmpty()) delay(TYPING_PAUSE)
            val found = cabinet.work.quiet("search places") { cabinet.useCases.searchPlaces(text) } ?: return@restart
            screen.update { it.copy(places = found) }
        }
    }

    /**
     * Свой заводской номер для своей кассы.
     *
     * Номер своей кассе присваивает касса приложения, и в кабинете его
     * не набирают, а получают. Чужая касса приезжает с номером на корпусе,
     * и его вводит владелец.
     */
    fun stamp(draft: RegisterDraft) {
        viewModelScope.launch {
            val info = cabinet.useCases.issueFactoryNumber()
                .shown(cabinet.texts.register.factoryNumber, "generate factory number", cabinet.talk) ?: return@launch
            if (draft.factory.isNotBlank()) return@launch
            draft.factory = info.factoryNumber
            draft.year = info.manufactureYear.toString()
        }
    }

    /**
     * Заводит кассу; удалось — черновик очищается, кроме выданного номера,
     * и кассы окна перечитываются.
     */
    fun add(draft: RegisterDraft, known: FactoryStamp?, onAdded: (CabinetRegister) -> Unit) {
        val place = draft.place ?: return
        val model = draft.model ?: return
        val create = RegisterCreate(
            retailPlaceId = place.id,
            modelCode = model.modelCode,
            factoryNumber = draft.factory,
            manufactureYear = draft.year.toInt(),
            internalName = draft.name
        )
        viewModelScope.launch {
            val created = cabinet.work.run("add register") { cabinet.useCases.addRegister(create) }.value
                ?: return@launch
            draft.factory = known?.number.orEmpty()
            draft.name = ""
            // Заведённая касса известна по ответу кабинета: список касс
            // сети ради неё не перечитывается — это сотни обращений.
            cabinet.registerChanged(created)
            onAdded(created)
        }
    }
}

/** Пауза набора, после которой кабинет спрашивается о точке: не на каждый знак. */
private val TYPING_PAUSE = 300.milliseconds

/** Модель заведения кассы окна: справочник моделей один на всё окно. */
@Composable
internal fun addRegisterViewModel(cabinet: CabinetViewModel): AddRegisterViewModel =
    viewModel { AddRegisterViewModel(cabinet) }
