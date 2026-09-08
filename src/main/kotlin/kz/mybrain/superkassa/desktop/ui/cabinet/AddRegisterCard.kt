package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetRegister
import kz.mybrain.superkassa.desktop.server.cabinet.KkmModel
import kz.mybrain.superkassa.desktop.server.cabinet.RegisterCreate
import kz.mybrain.superkassa.desktop.server.cabinet.RetailPlace
import kz.mybrain.superkassa.desktop.server.cabinet.addRegister
import kz.mybrain.superkassa.desktop.server.cabinet.kkmModels
import kz.mybrain.superkassa.desktop.server.cabinet.retailPlaces
import kz.mybrain.superkassa.desktop.ui.components.BusyButton
import kz.mybrain.superkassa.desktop.ui.components.LabelledPicker
import kz.mybrain.superkassa.desktop.ui.components.SectionCard
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts

/**
 * Заведение кассы в кабинете — одной формой для раздела и для мастера.
 *
 * Заведённая касса — ещё не поставленная на учёт: она получает состояние
 * «черновик» и ждёт заявления в ИСНА. Модель и торговая точка выбираются
 * из справочников кабинета: произвольные значения ИСНА не примет, и отказ
 * пришёл бы уже после подписи заявления.
 *
 * Под кнопкой перечислено недостающее. Прежде она просто не нажималась,
 * и владелец перебирал поля, гадая, какое из пяти пустое.
 */
@Composable
fun AddRegisterCard(
    cabinet: CabinetSession,
    texts: CabinetTexts,
    known: FactoryStamp? = null,
    onAdded: (CabinetRegister) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val draft = remember(known) { RegisterDraft(known) }
    var places by remember { mutableStateOf<List<RetailPlace>>(emptyList()) }
    var models by remember { mutableStateOf<List<KkmModel>>(emptyList()) }

    LaunchedEffect(cabinet.token) {
        val token = cabinet.token ?: return@LaunchedEffect
        places = cabinet.guard { cabinet.client.retailPlaces(token) }?.items.orEmpty()
        models = cabinet.guard { cabinet.client.kkmModels(token) }?.items.orEmpty()
    }

    SectionCard(title = texts.addRegister) {
        RegisterFields(texts, draft, places, models, stamped = known != null)
        val missing = missingFields(texts, draft)
        BusyButton(
            text = texts.addRegister,
            busy = cabinet.busy,
            enabled = missing.isEmpty(),
            modifier = Modifier.fillMaxWidth(),
            onClick = { scope.launch { add(cabinet, draft, known, onAdded) } }
        )
        MissingLine(texts, missing)
    }
}

/**
 * Поля заводимой кассы.
 *
 * @param stamped выданы ли заводской номер и год узлом: в мастере они
 *   получены на первом шаге и правке не подлежат.
 */
@Composable
private fun RegisterFields(
    texts: CabinetTexts,
    draft: RegisterDraft,
    places: List<RetailPlace>,
    models: List<KkmModel>,
    stamped: Boolean
) {
    LabelledPicker(
        label = texts.place,
        options = places,
        selected = draft.place,
        title = { it?.name.orEmpty() },
        onSelect = { draft.place = it }
    )
    LabelledPicker(
        label = texts.model,
        options = models,
        selected = draft.model,
        title = { it?.name ?: it?.modelCode.orEmpty() },
        onSelect = { draft.model = it }
    )
    if (!stamped) {
        FormField(texts.factoryNumber, draft.factory, texts.required) { draft.factory = it }
        FormField(texts.manufactureYear, draft.year, texts.required) {
            draft.year = it.filter(Char::isDigit).take(YEAR_DIGITS)
        }
    }
    FormField(texts.internalName, draft.name, texts.internalNameHint) { draft.name = it }
}

/** Поле формы во всю ширину карточки с подписью под ним. */
@Composable
private fun FormField(label: String, value: String, hint: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        supportingText = { Text(hint) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

/** Чего не хватает для заведения кассы — словами, а не погашенной кнопкой. */
@Composable
private fun MissingLine(texts: CabinetTexts, missing: List<String>) {
    if (missing.isEmpty()) return
    Text(
        text = "${texts.missing}: ${missing.joinToString(", ")}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

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
