package kz.mybrain.superkassa.presentation.cabinet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.data.cabinet.RetailPlace
import kz.mybrain.superkassa.presentation.adaptive.ContentKind
import kz.mybrain.superkassa.presentation.adaptive.contentWidth
import kz.mybrain.superkassa.presentation.components.EmptyState
import kz.mybrain.superkassa.presentation.session.CabinetSession
import kz.mybrain.superkassa.presentation.session.Session
import kz.mybrain.superkassa.presentation.strings.CabinetTexts
import kz.mybrain.superkassa.presentation.theme.AppIcons
import kz.mybrain.superkassa.presentation.theme.Spacing

/**
 * Справа — выбранная касса целиком, а до выбора кассы сама точка.
 *
 * Карточка не шире строки чтения и встаёт от левого края: на широком
 * мониторе она тянулась на всё, что оставила колонка, и поле «Регион»
 * выходило в две тысячи точек, а кнопка «Сменить адрес» уезжала от него
 * на другой край экрана.
 *
 * @param onBack возврат к списку точек, когда карточка сменила его
 *   на узком окне; `null` — список стоит рядом, и возвращаться некуда.
 */
@Composable
internal fun PlaceDetail(
    session: Session,
    cabinet: CabinetSession,
    texts: CabinetTexts,
    places: List<RetailPlace>,
    place: String?,
    register: String?,
    onBack: (() -> Unit)?,
    onChanged: () -> Unit
) {
    val chosen = cabinet.registers.firstOrNull { it.id == register }
    val chosenPlace = places.firstOrNull { it.id == place }
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(Spacing.tight)) {
        if (onBack != null) BackToPlaces(texts, onBack)
        val pane = Modifier.weight(1f).contentWidth(ContentKind.Reading)
        when {
            chosen != null -> RegisterDetails(session, cabinet, texts, chosen, modifier = pane)
            chosenPlace != null -> PlaceCard(session, cabinet, texts, chosenPlace, pane, onChanged)
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
