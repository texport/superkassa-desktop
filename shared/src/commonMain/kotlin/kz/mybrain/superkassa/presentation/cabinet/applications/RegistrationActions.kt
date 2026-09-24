package kz.mybrain.superkassa.presentation.cabinet.applications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import kz.mybrain.superkassa.designsystem.button.BusyButton
import kz.mybrain.superkassa.designsystem.button.FieldButtonKind
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.domain.cabinet.model.ApplicationStage
import kz.mybrain.superkassa.domain.cabinet.model.RetailPlace
import kz.mybrain.superkassa.presentation.cabinet.CabinetViewModel
import kz.mybrain.superkassa.presentation.cabinet.register.RegisterView
import kz.mybrain.superkassa.presentation.cabinet.register.availableActions
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.cabinet.CabinetTexts

/**
 * Заявления в ИСНА: постановка на учёт, перерегистрация, снятие с учёта.
 *
 * Все три идут одним путём: кабинет готовит заявление и отдаёт то, что
 * нужно подписать, NCALayer подписывает ключом владельца, кабинет
 * отправляет подписанное в ИСНА. Поэтому и подача одна — `CabinetApplications.submit`, —
 * а различаются только подготовка и отправка.
 *
 * Кнопка подачи названа подачей, а не видом заявления: прежде она носила
 * подпись выбранного сегмента, и ряд читался как «выберите одно из трёх»
 * с той же надписью внизу — было неясно, где выбор, а где действие.
 */
@Composable
fun RegistrationActionsBlock(
    cabinet: CabinetViewModel,
    language: Language,
    texts: CabinetTexts,
    view: RegisterView,
    onDone: () -> Unit
) {
    val register = view.row
    val model = applicationViewModel(cabinet)
    val state by model.state.collectAsState()
    val window by cabinet.state.collectAsState()
    LaunchedEffect(register.id, register.status) { model.show(register) }
    val available = availableActions(register)
    if (available.isEmpty()) {
        NoActions(register, texts)
    } else {
        KindChoice(state.form, available, texts, model::edit)
        ApplicationEditor(state.form, window.places, language, texts, model::edit)
        if (state.stage == ApplicationStage.Signing) {
            // Пока подписывающий ждёт подпись, на месте кнопки идёт отсчёт
            // срока с отменой — тот же, что на двери входа.
            ApplicationSignWait(language, texts, model::cancel)
        } else {
            val enabled = state.form.kind in available && state.form.filled
            SubmitRow(state, window.busy, enabled, texts) { model.submit(register, view.state.here, onDone) }
        }
    }
    ApplicationResult(state.outcome, texts)
    // Кабинет отказал из-за открытой смены — под причиной стоит то, что её чинит;
    // пока подача идёт, отказа на экране нет, и блок пуст.
    ShiftBlock(state, texts, view.state.here, model) { pin, here ->
        model.closeShiftAndSubmit(pin, register, here, onDone)
    }
}

/**
 * Вид заявления: подаётся только то, что по нынешнему состоянию кассы ИСНА примет.
 *
 * Отборными чипами, которые переносятся на новую строку, а не сегментами:
 * три сегмента равной ширины по самой длинной подписи в карточку кассы
 * не входили, ряд ужимался, и владелец читал «Поставить на учё»
 * и «Перерегистриро».
 */
@Composable
private fun KindChoice(
    form: ApplicationForm,
    available: Set<ActionKind>,
    texts: CabinetTexts,
    onEdit: (ApplicationForm) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.buttonGap),
        verticalArrangement = Arrangement.spacedBy(Spacing.itemGap)
    ) {
        ActionKind.entries.forEach { kind ->
            val chosen = kind == form.kind
            FilterChip(
                selected = chosen,
                enabled = kind in available,
                onClick = { onEdit(form.copy(kind = kind)) },
                label = { Text(kind.title(texts)) },
                leadingIcon = if (chosen) ({ Icon(AppIcons.chosen, contentDescription = null) }) else null
            )
        }
    }
}

/** То, что к заявлению надо указать: точку перерегистрации или причину снятия. */
@Composable
private fun ApplicationEditor(
    form: ApplicationForm,
    places: List<RetailPlace>,
    language: Language,
    texts: CabinetTexts,
    onEdit: (ApplicationForm) -> Unit
) {
    ApplicationFields(
        kind = form.kind,
        texts = texts,
        language = language,
        places = places,
        placeId = form.placeId,
        reason = form.reason,
        comment = form.comment,
        onPlace = { onEdit(form.copy(placeId = it)) },
        onReason = { onEdit(form.copy(reason = it)) },
        onComment = { onEdit(form.copy(comment = it)) }
    )
}

/**
 * Кнопка подачи.
 *
 * Пока отказ по открытой смене стоит на экране, главным действием
 * становится то, которое его чинит: повторная подача кончится тем же
 * отказом, а две залитые кнопки подряд не говорят, какую нажимать.
 */
@Composable
private fun SubmitRow(
    state: ApplicationUiState,
    busy: Boolean,
    enabled: Boolean,
    texts: CabinetTexts,
    onSubmit: () -> Unit
) {
    BusyButton(
        text = state.stage?.title(texts) ?: texts.submitApplication,
        busy = state.stage != null || busy,
        enabled = enabled,
        kind = if (state.blockedByShift) FieldButtonKind.Tonal else FieldButtonKind.Filled,
        onClick = onSubmit
    )
    // Погашенная кнопка сама не говорит, чего ей не хватает.
    if (!state.form.filled) Note(texts.hints.newPlaceNotChosen)
}

/**
 * Кабинет отказал из-за открытой смены — спрашиваем прямо здесь, а не
 * оставляем владельца идти закрывать её окольным путём. Закрыть смену можно
 * лишь у кассы этой машины: смену чужой кассы здесь не видно.
 *
 * @param here касса этой машины под этой кассой кабинета; `null` — её здесь нет.
 */
@Composable
private fun ShiftBlock(
    state: ApplicationUiState,
    texts: CabinetTexts,
    here: KkmResponse?,
    model: ApplicationViewModel,
    onConfirm: (String, KkmResponse) -> Unit
) {
    if (!state.blockedByShift) return
    if (here == null) {
        Note(texts.shiftOpenElsewhere)
        return
    }
    BusyButton(text = texts.closeShiftAndDeregister, busy = state.closingShift) { model.closing(true) }
    if (state.closing) {
        CloseShiftBeforeDeregister(texts, state.closingShift, onDismiss = { model.closing(false) }) { pin ->
            onConfirm(pin, here)
        }
    }
}
