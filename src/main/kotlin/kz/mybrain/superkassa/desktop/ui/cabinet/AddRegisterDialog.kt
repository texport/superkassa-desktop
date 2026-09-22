package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetRegister
import kz.mybrain.superkassa.desktop.server.cabinet.KkmModel
import kz.mybrain.superkassa.desktop.server.cabinet.RegisterCreate
import kz.mybrain.superkassa.desktop.server.cabinet.addRegister
import kz.mybrain.superkassa.desktop.server.cabinet.allKkmModels
import kz.mybrain.superkassa.desktop.server.factoryInfo
import kz.mybrain.superkassa.desktop.ui.components.FormDialog
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons

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
    session: Session,
    cabinet: CabinetSession,
    texts: CabinetTexts,
    known: FactoryStamp? = null,
    onDismiss: () -> Unit,
    onAdded: (CabinetRegister) -> Unit
) {
    val scope = rememberCoroutineScope()
    val draft = remember(known) { RegisterDraft(known) }
    val issued = ourModel(draft.model)
    // Точки берутся у сеанса, а не читаются окном заново: список точек
    // компании один, и своя копия здесь расходилась с ним — только что
    // созданная точка появлялась в окне, но не в заявлении.
    val places = cabinet.places
    var models by remember { mutableStateOf<List<KkmModel>>(emptyList()) }
    var addingPlace by remember { mutableStateOf(false) }

    LaunchedEffect(cabinet.token) {
        val token = cabinet.token ?: return@LaunchedEffect
        cabinet.refreshPlaces()
        models = cabinet.guard { cabinet.client.allKkmModels(token) }.orEmpty()
    }

    LaunchedEffect(draft.model?.modelCode) {
        if (!issued || draft.factory.isNotBlank()) return@LaunchedEffect
        session.guard(texts.factoryNumber) { session.client.factoryInfo() }?.let { info ->
            draft.factory = info.factoryNumber
            draft.year = info.manufactureYear.toString()
        }
    }

    FormDialog(
        title = texts.addRegister,
        icon = AppIcons.kkm,
        action = texts.addRegister,
        close = texts.close,
        busy = cabinet.busy,
        missing = missingFields(texts, draft),
        onDismiss = onDismiss,
        onAction = {
            scope.launch {
                add(cabinet, draft, known) { created ->
                    onAdded(created)
                    onDismiss()
                }
            }
        }
    ) {
        RegisterFields(
            texts = texts,
            language = session.language,
            draft = draft,
            places = places,
            models = models,
            stamped = known != null,
            issued = issued,
            onCreatePlace = { addingPlace = true }
        )
    }
    if (addingPlace) {
        AddPlaceCard(
            session = session,
            cabinet = cabinet,
            texts = texts,
            onDismiss = { addingPlace = false },
            onAdded = { addingPlace = false }
        )
    }
}

/**
 * Наша ли это модель.
 *
 * Заводской номер своей кассе присваивает узел при выпуске, и в кабинете
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

/** Заводит кассу и очищает черновик, оставив то, что выдал узел. */
private suspend fun add(
    cabinet: CabinetSession,
    draft: RegisterDraft,
    known: FactoryStamp?,
    onAdded: (CabinetRegister) -> Unit
) {
    val token = cabinet.token ?: return
    val place = draft.place ?: return
    val model = draft.model ?: return
    val created = cabinet.guard {
        cabinet.client.addRegister(
            token,
            RegisterCreate(
                retailPlaceId = place.id,
                modelCode = model.modelCode,
                factoryNumber = draft.factory.trim(),
                manufactureYear = draft.year.toInt(),
                internalName = draft.name.trim().takeIf { it.isNotBlank() }
            )
        )
    } ?: return
    draft.factory = known?.number.orEmpty()
    draft.name = ""
    cabinet.refreshRegisters()
    onAdded(created)
}
