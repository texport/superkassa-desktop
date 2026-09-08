package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
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
import kz.mybrain.superkassa.desktop.server.cabinet.ApplicationSent
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetRegister
import kz.mybrain.superkassa.desktop.server.cabinet.RetailPlace
import kz.mybrain.superkassa.desktop.server.cabinet.retailPlaces
import kz.mybrain.superkassa.desktop.ui.components.BusyButton
import kz.mybrain.superkassa.desktop.ui.components.ChoiceSegments
import kz.mybrain.superkassa.desktop.ui.components.DetailLine
import kz.mybrain.superkassa.desktop.ui.components.LabelledPicker
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Заявления в ИСНА: постановка на учёт, перерегистрация, снятие с учёта.
 *
 * Все три идут одним путём: кабинет готовит заявление и отдаёт то, что
 * нужно подписать, NCALayer подписывает ключом владельца, кабинет
 * отправляет подписанное в ИСНА. Поэтому и код один — [submitApplication], —
 * а различаются только подготовка и отправка.
 *
 * Кнопка подачи названа подачей, а не видом заявления: прежде она носила
 * подпись выбранного сегмента, и ряд читался как «выберите одно из трёх»
 * с той же надписью внизу — было неясно, где выбор, а где действие.
 */
@Composable
fun RegistrationActionsBlock(
    cabinet: CabinetSession,
    texts: CabinetTexts,
    register: CabinetRegister,
    onDone: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var kind by remember(register.id) { mutableStateOf(ActionKind.Registration) }
    var reason by remember(register.id) { mutableStateOf(DeregistrationReason.CessationOfUse) }
    var comment by remember(register.id) { mutableStateOf("") }
    var placeId by remember(register.id) { mutableStateOf("") }
    var sent by remember(register.id) { mutableStateOf<ApplicationSent?>(null) }
    var places by remember { mutableStateOf<List<RetailPlace>>(emptyList()) }

    LaunchedEffect(cabinet.token) {
        val token = cabinet.token ?: return@LaunchedEffect
        places = cabinet.guard { cabinet.client.retailPlaces(token) }?.items.orEmpty()
    }

    ChoiceSegments(
        options = ActionKind.entries,
        selected = kind,
        label = { it.title(texts) },
        onSelect = { kind = it }
    )
    ApplicationFields(kind, texts, places, placeId, reason, comment, { placeId = it }, { reason = it }) {
        comment = it
    }
    BusyButton(text = texts.submitApplication, busy = cabinet.busy) {
        scope.launch {
            sent = submitApplication(cabinet, kind, register.id, placeId, reason, comment)
            onDone()
        }
    }
    ApplicationResult(sent, texts)
}

/** Что нужно уточнить у выбранного вида заявления. */
@Composable
private fun ApplicationFields(
    kind: ActionKind,
    texts: CabinetTexts,
    places: List<RetailPlace>,
    placeId: String,
    reason: DeregistrationReason,
    comment: String,
    onPlace: (String) -> Unit,
    onReason: (DeregistrationReason) -> Unit,
    onComment: (String) -> Unit
) {
    when (kind) {
        // Постановке на учёт уточнять нечего: всё нужное уже в паспорте кассы.
        ActionKind.Registration -> Unit
        // Точка выбирается из списка компании: прежде здесь стоял ввод
        // идентификатора, а взять его владельцу было неоткуда.
        ActionKind.Reregistration -> LabelledPicker(
            label = texts.newPlace,
            options = places,
            selected = places.firstOrNull { it.id == placeId },
            title = { it?.name.orEmpty() },
            onSelect = { onPlace(it.id) }
        )
        ActionKind.Deregistration -> DeregistrationFields(texts, reason, comment, onReason, onComment)
    }
}

/** Причина и пояснение к снятию с учёта. */
@Composable
private fun DeregistrationFields(
    texts: CabinetTexts,
    reason: DeregistrationReason,
    comment: String,
    onReason: (DeregistrationReason) -> Unit,
    onComment: (String) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
        verticalArrangement = Arrangement.spacedBy(Spacing.tight)
    ) {
        ChoiceSegments(
            options = DeregistrationReason.entries,
            selected = reason,
            label = { it.title(texts) },
            onSelect = onReason
        )
        OutlinedTextField(
            value = comment,
            onValueChange = onComment,
            label = { Text(texts.comment) },
            supportingText = { Text(texts.optional) },
            singleLine = true,
            modifier = Modifier.width(Sizes.fieldName)
        )
    }
}

/**
 * Чем закончилась подача.
 *
 * Здесь стояли два кода протокола через точку — `SENT · KKM_INACTIVE`.
 * Ответ ИСНА приходит не сразу, и об этом сказано прямо: иначе состояние
 * «отправлено» читается как незавершённая работа приложения.
 */
@Composable
private fun ApplicationResult(sent: ApplicationSent?, texts: CabinetTexts) {
    val done = sent ?: return
    DetailLine(texts.applicationSent, statusTitle(done.actionStatus, texts))
    DetailLine(texts.registerStatus, done.cashRegisterStatus?.let { statusTitle(it, texts) })
    Text(
        text = texts.applicationWait,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
