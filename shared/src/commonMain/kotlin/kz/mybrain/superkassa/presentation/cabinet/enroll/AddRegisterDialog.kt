package kz.mybrain.superkassa.presentation.cabinet.enroll

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.domain.cabinet.model.KkmModel
import kz.mybrain.superkassa.domain.cabinet.model.RetailPlace
import kz.mybrain.superkassa.presentation.cabinet.CabinetWindow
import kz.mybrain.superkassa.presentation.cabinet.places.AddPlaceCard
import kz.mybrain.superkassa.presentation.common.dialog.FormDialog
import kz.mybrain.superkassa.presentation.common.model.collectAsScreenState
import kz.mybrain.superkassa.presentation.strings.cabinet.CabinetTexts
import kz.mybrain.superkassa.presentation.strings.common.LocalLanguage
import kz.mybrain.superkassa.presentation.theme.icon.AppIcons

/**
 * Заведение кассы в кабинете — одной формой для мастера и для окна.
 *
 * Заведённая касса — ещё не поставленная на учёт: она получает состояние
 * «черновик» и ждёт заявления в ИСНА. Модель и торговая точка выбираются
 * из справочников кабинета: произвольные значения ИСНА не примет, и отказ
 * пришёл бы уже после подписи заявления.
 *
 * Пока обязательное не заполнено, кнопка погашена и ничего под собой
 * не объясняет: незаполненные поля видно по самой форме, и перечень
 * под кнопкой повторял их названия второй раз.
 *
 * Форма живёт окном: пять полей в узкой колонке отжимали список наверх
 * и рвали его вёрстку, а кассу создают раз в жизни. Окно одно и на раздел
 * касс, и на мастер подключения — две формы разошлись бы на первой правке.
 */
@Composable
fun AddRegisterDialog(
    cabinet: CabinetWindow,
    texts: CabinetTexts,
    known: FactoryStamp? = null,
    onDismiss: () -> Unit,
    onAdded: (CabinetRegister) -> Unit
) {
    val model = addRegisterViewModel(cabinet.cabinet)
    val window by cabinet.cabinet.state.collectAsScreenState()
    val draft = remember(known) { RegisterDraft(known) }
    var addingPlace by remember { mutableStateOf(false) }
    OpenForm(model, draft)
    FormDialog(
        title = texts.addRegister,
        icon = AppIcons.kkm,
        action = texts.addRegister,
        close = texts.close,
        busy = window.busy,
        missing = missingFields(texts, draft),
        onDismiss = onDismiss,
        onAction = { model.add(draft, known) { created -> onAdded(created).also { onDismiss() } } }
    ) {
        // Точки берутся у кабинета окна, а не читаются окном заново: список точек
        // компании один, и своя копия здесь расходилась с ним — только что
        // созданная точка появлялась в окне, но не в заявлении.
        Fields(texts, model, draft, window.places, known != null) { addingPlace = true }
    }
    if (addingPlace) {
        AddPlaceCard(cabinet, texts, onDismiss = { addingPlace = false }, onAdded = { addingPlace = false })
    }
}

/** Справочник моделей читается, как форма открыта; свою кассу номером штампует касса приложения. */
@Composable
private fun OpenForm(model: AddRegisterViewModel, draft: RegisterDraft) {
    LaunchedEffect(Unit) { model.open() }
    LaunchedEffect(draft.model?.modelCode) {
        if (ourModel(draft.model) && draft.factory.isBlank()) model.stamp(draft)
    }
}

/** Поля формы: справочник моделей — у модели заведения, точки — у кабинета окна. */
@Composable
private fun Fields(
    texts: CabinetTexts,
    model: AddRegisterViewModel,
    draft: RegisterDraft,
    places: List<RetailPlace>,
    stamped: Boolean,
    onCreatePlace: () -> Unit
) {
    val models by model.state.collectAsScreenState()
    RegisterFields(
        texts = texts,
        language = LocalLanguage.current,
        draft = draft,
        places = places,
        models = models,
        stamped = stamped,
        issued = ourModel(draft.model),
        onCreatePlace = onCreatePlace
    )
}

/**
 * Наша ли это модель.
 *
 * Заводской номер своей кассе присваивает касса приложения при выпуске, и в кабинете
 * его не набирают, а получают. Чужая касса приезжает с номером на корпусе,
 * и его вводит владелец.
 *
 * Модель узнаётся по наименованию из справочника: своего признака
 * «наша модель» справочник ИСНА не отдаёт.
 */
private fun ourModel(model: KkmModel?): Boolean =
    model?.name.orEmpty().contains(OUR_MODEL, ignoreCase = true)

/** Как называется своя касса в справочнике моделей. */
private const val OUR_MODEL = "Суперкасса"

/** Какие обязательные поля пусты. */
private fun missingFields(texts: CabinetTexts, draft: RegisterDraft): List<String> = listOfNotNull(
    texts.place.takeIf { draft.place == null },
    texts.model.takeIf { draft.model == null },
    texts.factoryNumber.takeIf { draft.factory.isBlank() },
    texts.manufactureYear.takeIf { draft.year.length != YEAR_DIGITS }
)
