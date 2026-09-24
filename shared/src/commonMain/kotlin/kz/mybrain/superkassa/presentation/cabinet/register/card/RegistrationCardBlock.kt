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
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.domain.cabinet.model.documents.RegistrationCard
import kz.mybrain.superkassa.presentation.cabinet.CabinetViewModel
import kz.mybrain.superkassa.presentation.cabinet.statusTitle
import kz.mybrain.superkassa.presentation.common.format.Dates
import kz.mybrain.superkassa.presentation.common.section.DetailLine
import kz.mybrain.superkassa.presentation.common.state.ScreenSlot
import kz.mybrain.superkassa.presentation.common.state.ScreenState
import kz.mybrain.superkassa.presentation.strings.cabinet.CabinetTexts
import kz.mybrain.superkassa.presentation.theme.icon.AppIcons
import kz.mybrain.superkassa.presentation.theme.size.Spacing

/**
 * Регистрационная карта кассы.
 *
 * Карту выдаёт КГД после постановки на учёт; кабинет хранит её и печатает
 * в PDF. Пока карты нет, раздел так и говорит: погашенная кнопка
 * «Сохранить PDF» над несуществующим документом обещала бы то, чего нет.
 */
@Composable
fun RegistrationCardBlock(cabinet: CabinetViewModel, texts: CabinetTexts, register: CabinetRegister) {
    val model = registrationCardViewModel(cabinet)
    val state by model.state.collectAsState()
    val window by cabinet.state.collectAsState()
    LaunchedEffect(register.id, register.registrationCardAvailable) { model.show(register) }
    val issued = state.card?.takeIf { state.registerId == register.id }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.snug)) {
        ScreenSlot(cardState(issued, register, state, texts), dense = true) {
            if (issued == null) return@ScreenSlot
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DetailLine(statusTitle(issued.status ?: register.status, texts), Dates.momentOf(issued.updatedAt))
                FilledTonalButton(enabled = !window.busy, onClick = { model.save(register) }) { Text(texts.savePdf) }
            }
        }
        if (!register.registrationNumber.isNullOrBlank()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            RegistrationCardVersions(state, texts, register, window.busy, model)
        }
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
    else -> ScreenState.Empty(AppIcons.print, texts.cardMissing, texts.hints.cardMissing)
}
