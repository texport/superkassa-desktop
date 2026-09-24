package kz.mybrain.superkassa.presentation.cabinet.enroll

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.domain.cabinet.model.KkmModel
import kz.mybrain.superkassa.domain.cabinet.model.RegisterCreate
import kz.mybrain.superkassa.presentation.cabinet.CabinetViewModel
import kz.mybrain.superkassa.presentation.cabinet.value
import kz.mybrain.superkassa.presentation.common.model.shown

/**
 * Заведение кассы в кабинете: справочник моделей, свой заводской номер
 * и сама касса.
 *
 * Заведённая касса — ещё не поставленная на учёт: она получает состояние
 * «черновик» и ждёт заявления в ИСНА. Модель и точка выбираются из
 * справочников кабинета: произвольные значения ИСНА не примет.
 */
class AddRegisterViewModel(private val cabinet: CabinetViewModel) : ViewModel() {
    private val models = MutableStateFlow<List<KkmModel>>(emptyList())

    /** Справочник моделей касс: читается один раз на окно — он не меняется. */
    val state: StateFlow<List<KkmModel>> = models.asStateFlow()

    /** Справочник читается, когда форму открыли, и один раз, пока он не прочитан. */
    fun open() {
        if (models.value.isNotEmpty()) return
        viewModelScope.launch {
            cabinet.work.run("read kkm models") { cabinet.useCases.readModels() }.value?.let { models.value = it }
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
                .shown(cabinet.texts.factoryNumber, "generate factory number", cabinet.talk) ?: return@launch
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
            cabinet.reload()
            onAdded(created)
        }
    }
}

/** Модель заведения кассы окна: справочник моделей один на всё окно. */
@Composable
fun addRegisterViewModel(cabinet: CabinetViewModel): AddRegisterViewModel =
    viewModel { AddRegisterViewModel(cabinet) }
