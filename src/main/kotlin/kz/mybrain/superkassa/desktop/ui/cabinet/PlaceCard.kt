package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.cabinet.RetailPlace
import kz.mybrain.superkassa.desktop.server.cabinet.removeRetailPlace
import kz.mybrain.superkassa.desktop.ui.components.DetailLine
import kz.mybrain.superkassa.desktop.ui.components.InfoTip
import kz.mybrain.superkassa.desktop.ui.components.ScrollableColumn
import kz.mybrain.superkassa.desktop.ui.components.SectionCard
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Торговая точка: что о ней записано и что с ней можно сделать.
 *
 * Открывается справа, когда точка выбрана, а касса ещё нет. Правка живёт
 * здесь же — переименование и переезд относятся ровно к тем строкам,
 * что записаны выше, и отдельного раздела им не нужно.
 */
@Composable
fun PlaceCard(
    session: Session,
    cabinet: CabinetSession,
    texts: CabinetTexts,
    place: RetailPlace,
    modifier: Modifier = Modifier,
    onChanged: () -> Unit
) {
    val scope = rememberCoroutineScope()
    ScrollableColumn(modifier = modifier.fillMaxWidth(), spacing = Spacing.snug) {
        SectionCard(
            title = texts.places,
            info = texts.hints.places,
            trailing = { PlaceRemoval(cabinet, texts, place, onChanged) }
        ) {
            Text(
                text = place.name,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.hairline)
            ) {
                DetailLine(texts.placeAddress, addressIn(session.language, place.address, place.addressKz))
                DetailLine(texts.registerCount, place.cashRegisterCount.toString())
            }
            PlaceEditRow(session, cabinet, texts, place) { scope.launch { onChanged() } }
        }
    }
}

/**
 * Удаление точки — или объяснение, почему его нет.
 *
 * Погашенная кнопка молчит о причине: владелец видел, что точку удалить
 * нельзя, но не знал, что мешает именно привязанная касса.
 */
@Composable
private fun PlaceRemoval(
    cabinet: CabinetSession,
    texts: CabinetTexts,
    place: RetailPlace,
    onChanged: () -> Unit
) {
    val scope = rememberCoroutineScope()
    if (place.cashRegisterCount > 0) {
        InfoTip(texts.placeRemoveBlocked)
        return
    }
    OutlinedButton(
        enabled = !cabinet.busy,
        onClick = {
            scope.launch {
                val token = cabinet.token ?: return@launch
                if (cabinet.guard { cabinet.client.removeRetailPlace(token, place.id) } != null) onChanged()
            }
        }
    ) { Text(texts.remove) }
}
