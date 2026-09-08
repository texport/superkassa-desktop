package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetRegister
import kz.mybrain.superkassa.desktop.server.cabinet.KkmModel
import kz.mybrain.superkassa.desktop.server.cabinet.RegisterCreate
import kz.mybrain.superkassa.desktop.server.cabinet.RetailPlace
import kz.mybrain.superkassa.desktop.server.cabinet.addRegister
import kz.mybrain.superkassa.desktop.server.cabinet.kkmModels
import kz.mybrain.superkassa.desktop.server.cabinet.retailPlaces
import kz.mybrain.superkassa.desktop.server.factoryInfo
import kz.mybrain.superkassa.desktop.ui.components.BusyButton
import kz.mybrain.superkassa.desktop.ui.components.LabelledPicker
import kz.mybrain.superkassa.desktop.ui.components.SectionCard
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Заведение кассы в кабинете — одной формой для мастера и для окна.
 *
 * Заведённая касса — ещё не поставленная на учёт: она получает состояние
 * «черновик» и ждёт заявления в ИСНА. Модель и торговая точка выбираются
 * из справочников кабинета: произвольные значения ИСНА не примет, и отказ
 * пришёл бы уже после подписи заявления.
 *
 * Под кнопкой перечислено недостающее. Прежде она просто не нажималась,
 * и владелец перебирал поля, гадая, какое из пяти пустое.
 *
 * В разделе касс форма живёт отдельным окном, а не карточкой под списком:
 * пять полей в узкой колонке отжимали список наверх и рвали его вёрстку,
 * а заводят кассу раз в жизни. В мастере подключения она остаётся
 * карточкой — там это шаг, а не отступление от списка.
 */
@Composable
fun AddRegisterCard(
    session: Session,
    cabinet: CabinetSession,
    texts: CabinetTexts,
    known: FactoryStamp? = null,
    onAdded: (CabinetRegister) -> Unit = {}
) {
    SectionCard(title = texts.addRegister) {
        AddRegisterForm(session, cabinet, texts, known, Modifier.fillMaxWidth(), onAdded)
    }
}

/**
 * Заведение кассы окном.
 *
 * Окно закрывается только по удаче: на отказе кабинета владелец должен
 * видеть, что именно он выбрал, а не пустой список и погасшее окно.
 */
@Composable
fun AddRegisterDialog(
    session: Session,
    cabinet: CabinetSession,
    texts: CabinetTexts,
    onDismiss: () -> Unit,
    onAdded: (CabinetRegister) -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!cabinet.busy) onDismiss() },
        icon = { Icon(AppIcons.kkm, contentDescription = null) },
        title = { Text(texts.addRegister) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.snug)) {
                AddRegisterForm(session, cabinet, texts, known = null, modifier = Modifier.fillMaxWidth()) { created ->
                    onAdded(created)
                    onDismiss()
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(enabled = !cabinet.busy, onClick = onDismiss) { Text(texts.close) }
        }
    )
}

/**
 * Поля заводимой кассы, кнопка и перечень незаполненного.
 *
 * @param known заводской номер и год, выданные узлом: в мастере они
 *   получены на первом шаге и правке не подлежат.
 */
@Composable
private fun AddRegisterForm(
    session: Session,
    cabinet: CabinetSession,
    texts: CabinetTexts,
    known: FactoryStamp?,
    modifier: Modifier = Modifier,
    onAdded: (CabinetRegister) -> Unit
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

    // У своей модели заводской номер и год выдаёт узел — он их и присваивает
    // при выпуске. Владельцу их набирать неоткуда, а набранное от руки
    // разошлось бы с тем, что касса отнесёт в ОФД при регистрации.
    val ours = ourModel(draft.model)
    LaunchedEffect(draft.model?.modelCode) {
        if (!ours || draft.factory.isNotBlank()) return@LaunchedEffect
        session.guard(texts.factoryNumber) { session.client.factoryInfo() }?.let { info ->
            draft.factory = info.factoryNumber
            draft.year = info.manufactureYear.toString()
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.snug)) {
        RegisterFields(texts, draft, places, models, stamped = known != null, issued = ours)
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

/** Поля заводимой кассы. */
@Composable
private fun RegisterFields(
    texts: CabinetTexts,
    draft: RegisterDraft,
    places: List<RetailPlace>,
    models: List<KkmModel>,
    stamped: Boolean,
    issued: Boolean
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
        // Выданные узлом номер и год показаны погашенными: они уже
        // присвоены кассе, и правка сделала бы их неправдой.
        FormField(
            label = texts.factoryNumber,
            value = draft.factory,
            hint = if (issued) texts.factoryIssued else texts.required,
            enabled = !issued
        ) { draft.factory = it }
        FormField(
            label = texts.manufactureYear,
            value = draft.year,
            hint = if (issued) texts.factoryIssued else texts.required,
            enabled = !issued
        ) { draft.year = it.filter(Char::isDigit).take(YEAR_DIGITS) }
    }
    FormField(texts.internalName, draft.name, texts.internalNameHint) { draft.name = it }
}

/** Поле формы во всю ширину карточки с подписью под ним. */
@Composable
private fun FormField(
    label: String,
    value: String,
    hint: String,
    enabled: Boolean = true,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        supportingText = { Text(hint) },
        singleLine = true,
        enabled = enabled,
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
