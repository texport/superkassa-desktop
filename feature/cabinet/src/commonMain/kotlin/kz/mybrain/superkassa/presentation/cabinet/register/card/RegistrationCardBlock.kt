package kz.mybrain.superkassa.presentation.cabinet.register.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.format.Dates
import kz.mybrain.superkassa.designsystem.section.DetailLine
import kz.mybrain.superkassa.designsystem.state.ScreenSlot
import kz.mybrain.superkassa.designsystem.state.ScreenState
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.domain.cabinet.model.documents.RegistrationCard
import kz.mybrain.superkassa.presentation.cabinet.CabinetViewModel
import kz.mybrain.superkassa.presentation.words.cabinet.statusTitle
import kz.mybrain.superkassa.strings.api.cabinet.CabinetTexts

/**
 * Регистрационная карта кассы.
 *
 * Карту выдаёт КГД после постановки на учёт; кабинет хранит её и печатает
 * в PDF. Пока карты нет, раздел так и говорит: погашенная кнопка
 * «Сохранить PDF» над несуществующим документом обещала бы то, чего нет.
 */
@Composable
internal fun RegistrationCardBlock(cabinet: CabinetViewModel, texts: CabinetTexts, register: CabinetRegister) {
    val model = registrationCardViewModel(cabinet)
    val state by model.state.collectAsState()
    val window by cabinet.state.collectAsState()
    LaunchedEffect(register.id, register.registrationCardAvailable) { model.show(register) }
    val issued = state.card?.takeIf { state.registerId == register.id }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.fieldGap)) {
        ScreenSlot(cardState(issued, register, state, texts), dense = true) {
            if (issued != null) IssuedCard(issued, register, texts, window.busy) { model.save(register) }
        }
        if (!register.registrationNumber.isNullOrBlank()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            RegistrationCardVersions(state, texts, register, window.busy, model)
        }
    }
}

/** Выданная карта: её состояние, дата и сохранение в PDF. */
@Composable
private fun IssuedCard(
    issued: RegistrationCard,
    register: CabinetRegister,
    texts: CabinetTexts,
    busy: Boolean,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.fieldGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DetailLine(statusTitle(issued.status ?: register.status, texts), Dates.momentOf(issued.updatedAt))
        FilledTonalButton(enabled = !busy, onClick = onSave) { Text(texts.register.card.savePdf) }
    }
}

/** Карта есть, читается или её нет: объявленная кабинетом ждёт ответа, а не пустоты. */
private fun cardState(
    issued: RegistrationCard?,
    register: CabinetRegister,
    state: RegistrationCardUiState,
    texts: CabinetTexts
): ScreenState = when {
    issued != null -> ScreenState.Ready
    register.registrationCardAvailable && !state.cardAsked -> ScreenState.Working
    else -> ScreenState.Empty(AppIcons.print, texts.register.card.missing, texts.hints.cardMissing)
}
