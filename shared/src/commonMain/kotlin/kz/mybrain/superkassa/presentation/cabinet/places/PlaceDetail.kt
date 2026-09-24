package kz.mybrain.superkassa.presentation.cabinet.places

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.presentation.cabinet.CabinetWindow
import kz.mybrain.superkassa.presentation.cabinet.places.component.PlaceCard
import kz.mybrain.superkassa.presentation.cabinet.register.RegisterDetails
import kz.mybrain.superkassa.presentation.common.model.collectAsScreenState
import kz.mybrain.superkassa.presentation.common.state.EmptyState
import kz.mybrain.superkassa.presentation.theme.icon.AppIcons
import kz.mybrain.superkassa.presentation.theme.size.Spacing
import kz.mybrain.superkassa.strings.api.cabinet.CabinetTexts

/**
 * Справа — выбранная касса целиком, а до выбора кассы сама точка.
 *
 * Карточка занимает всю панель, которую оставил список: колонка шириной
 * чтения оставляла на широком окне пустую половину панели.
 *
 * @param onBack возврат к списку точек, когда карточка сменила его
 *   на узком окне; `null` — список стоит рядом, и возвращаться некуда.
 */
@Composable
internal fun PlaceDetail(
    cabinet: CabinetWindow,
    texts: CabinetTexts,
    selection: PlacesUiState,
    onBack: (() -> Unit)?
) {
    val window by cabinet.cabinet.state.collectAsScreenState()
    val chosen = window.registers.firstOrNull { it.id == selection.register }
    val chosenPlace = window.places.firstOrNull { it.id == selection.place }
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(Spacing.itemGap)) {
        if (onBack != null) BackToPlaces(texts, onBack)
        val pane = Modifier.weight(1f).fillMaxWidth()
        when {
            chosen != null -> RegisterDetails(cabinet, texts, chosen, modifier = pane)
            chosenPlace != null -> PlaceCard(cabinet, texts, chosenPlace, pane)
            else -> EmptyState(
                icon = AppIcons.newKkm,
                title = texts.pickRegisterFirst,
                hint = texts.hints.pickRegisterFirst,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Возврат к списку точек на узком окне.
 *
 * Стрелка с названием списка, куда она ведёт, — как навигация в шапке
 * по Material 3; своей строки шапки у колонки нет, поэтому кнопка
 * стоит над карточкой.
 */
@Composable
private fun BackToPlaces(texts: CabinetTexts, onBack: () -> Unit) {
    TextButton(onClick = onBack) {
        Icon(AppIcons.back, contentDescription = null, modifier = Modifier.padding(end = ButtonDefaults.IconSpacing))
        Text(texts.places)
    }
}
