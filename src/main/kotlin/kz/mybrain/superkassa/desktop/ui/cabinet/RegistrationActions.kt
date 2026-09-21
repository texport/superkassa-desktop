package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import kz.mybrain.superkassa.desktop.server.closeShift
import kz.mybrain.superkassa.desktop.ui.components.BusyButton
import kz.mybrain.superkassa.desktop.ui.components.ChoiceSegments
import kz.mybrain.superkassa.desktop.ui.components.DetailLine
import kz.mybrain.superkassa.desktop.ui.components.FieldButtonKind
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts

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
    session: Session,
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
    var outcome by remember(register.id) { mutableStateOf<ApplicationOutcome?>(null) }
    var stage by remember(register.id) { mutableStateOf<ApplicationStage?>(null) }
    var closingShift by remember(register.id) { mutableStateOf(false) }
    val places = cabinet.places

    LaunchedEffect(cabinet.token) {
        cabinet.refreshPlaces()
    }

    // Выбранным остаётся только то, что по нынешнему состоянию кассы
    // подаётся: иначе владелец возвращается к разделу и видит выбранным
    // заявление, которое ИСНА отвергнет.
    val available = availableActions(register)
    LaunchedEffect(register.status) {
        kind = available.firstOrNull() ?: return@LaunchedEffect
    }
    if (available.isEmpty()) {
        NoActions(register, texts)
        ApplicationResult(outcome, texts)
        return
    }
    ChoiceSegments(
        options = ActionKind.entries,
        selected = kind,
        label = { it.title(texts) },
        available = { it in available },
        onSelect = { kind = it }
    )
    ApplicationFields(kind, texts, places, placeId, reason, comment, { placeId = it }, { reason = it }) {
        comment = it
    }
    // Подача вынесена отдельно: её же повторяет окно закрытия смены,
    // и два вызова подряд разошлись бы на первой правке.
    val submit: suspend () -> Unit = {
        outcome = null
        outcome = submitApplication(cabinet, kind, register.id, placeId, reason, comment) { stage = it }
        stage = null
        onDone()
    }
    val shiftBlocks = outcome.blockedByShift()
    val closable = if (shiftBlocks) closableHere(register, session.kkms) else null
    // Заявление, которого кабинет не примет, и не подаётся: перерегистрация
    // без новой точки уходила в кабинет и возвращалась отказом, а поле
    // выбора при этом выглядело заполненным.
    val filled = kind != ActionKind.Reregistration || placeId.isNotBlank()
    // Пока отказ по открытой смене стоит на экране, главным действием
    // становится то, которое его чинит: повторная подача кончится тем же
    // отказом, а две залитые кнопки подряд не говорят, какую нажимать.
    BusyButton(
        text = stage?.title(texts) ?: texts.submitApplication,
        busy = cabinet.busy,
        enabled = kind in available && filled,
        kind = if (closable == null) FieldButtonKind.Filled else FieldButtonKind.Tonal
    ) {
        scope.launch { submit() }
    }
    // Погашенная кнопка сама не говорит, чего ей не хватает.
    if (!filled) Note(texts.hints.newPlaceNotChosen)
    ApplicationResult(outcome, texts)

    // Кабинет отказал из-за открытой смены — спрашиваем прямо здесь,
    // а не оставляем владельца идти закрывать её окольным путём.
    if (closable != null) {
        BusyButton(text = texts.closeShiftAndDeregister, busy = session.busy) { closingShift = true }
    } else if (shiftBlocks) {
        Note(texts.shiftOpenElsewhere)
    }
    if (closingShift && closable != null) {
        CloseShiftBeforeDeregister(texts, session.busy, onDismiss = { closingShift = false }) { pin ->
            scope.launch {
                val closed = session.guard(texts.closeShiftAndDeregister) {
                    session.client.closeShift(closable.kkmId, pin)
                }
                closingShift = false
                if (closed != null) submit()
            }
        }
    }
}

/** Помешала ли подаче открытая смена. */
private fun ApplicationOutcome?.blockedByShift(): Boolean =
    (this as? ApplicationOutcome.Failed)?.problem?.isShiftOpen() == true

/** Строка пояснения под кнопкой: её читают один раз и решают. */
@Composable
private fun Note(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** Почему заявлений сейчас нет — вместо ряда погашенных сегментов. */
@Composable
private fun NoActions(register: CabinetRegister, texts: CabinetTexts) {
    Text(
        text = noActionsReason(register, texts),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/**
 * Чем закончилась подача.
 *
 * Здесь стояли два кода протокола через точку — `SENT · KKM_INACTIVE`.
 * Ответ ИСНА приходит не сразу, и об этом сказано прямо: иначе состояние
 * «отправлено» читается как незавершённая работа приложения.
 */
@Composable
private fun ApplicationResult(outcome: ApplicationOutcome?, texts: CabinetTexts) {
    when (outcome) {
        null -> Unit
        is ApplicationOutcome.Sent -> {
            DetailLine(texts.applicationSent, statusTitle(outcome.sent.actionStatus, texts))
            DetailLine(texts.registerStatus, outcome.sent.cashRegisterStatus?.let { statusTitle(it, texts) })
            Text(
                text = texts.applicationWait,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        // Неудача остаётся под кнопкой до следующей подачи: всплывающая строка
        // каркаса гаснет за секунды, и владелец не успевал прочитать причину.
        is ApplicationOutcome.Failed -> Text(
            text = "${texts.applicationFailed}: ${cabinetMessage(outcome.problem, texts).words()}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
    }
}
