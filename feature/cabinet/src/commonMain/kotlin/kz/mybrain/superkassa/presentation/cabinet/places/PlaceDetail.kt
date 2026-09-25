package kz.mybrain.superkassa.presentation.cabinet.places

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.state.EmptyState
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.presentation.cabinet.CabinetWindow
import kz.mybrain.superkassa.presentation.cabinet.places.component.PlaceCard
import kz.mybrain.superkassa.presentation.cabinet.register.RegisterDetails
import kz.mybrain.superkassa.presentation.common.model.collectAsScreenState
import kz.mybrain.superkassa.strings.api.cabinet.CabinetTexts

/**
 * Справа — выбранная касса целиком, а до выбора кассы сама точка.
 *
 * Карточка занимает всю панель, которую оставил список: колонка шириной
 * чтения оставляла на широком окне пустую половину панели.
 *
 * Возврата к списку здесь нет: на узком окне карточка открыта шагом
 * истории окна, и назад ведёт стрелка в шапке окна.
 */
@Composable
internal fun PlaceDetail(
    cabinet: CabinetWindow,
    texts: CabinetTexts,
    selection: PlacesUiState
) {
    val window by cabinet.cabinet.state.collectAsScreenState()
    val chosen = window.registers.firstOrNull { it.id == selection.register }
    val chosenPlace = window.places.firstOrNull { it.id == selection.place }
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(Spacing.itemGap)) {
        val pane = Modifier.weight(1f).fillMaxWidth()
        when {
            chosen != null -> RegisterDetails(cabinet, texts, chosen, modifier = pane)
            chosenPlace != null -> PlaceCard(cabinet, texts, chosenPlace, pane)
            else -> EmptyState(
                icon = AppIcons.newKkm,
                title = texts.places.pickRegisterFirst,
                hint = texts.hints.pickRegisterFirst,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
