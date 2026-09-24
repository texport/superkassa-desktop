package kz.mybrain.superkassa.presentation.cabinet.places.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import kz.mybrain.superkassa.domain.cabinet.model.RetailPlace
import kz.mybrain.superkassa.presentation.cabinet.CabinetWindow
import kz.mybrain.superkassa.presentation.cabinet.addressIn
import kz.mybrain.superkassa.presentation.cabinet.places.placesViewModel
import kz.mybrain.superkassa.presentation.common.list.ScrollableColumn
import kz.mybrain.superkassa.presentation.common.message.InfoTip
import kz.mybrain.superkassa.presentation.common.model.collectAsScreenState
import kz.mybrain.superkassa.presentation.common.section.DetailLine
import kz.mybrain.superkassa.presentation.common.section.SectionCard
import kz.mybrain.superkassa.presentation.common.strings.LocalLanguage
import kz.mybrain.superkassa.presentation.theme.icon.Glyphs
import kz.mybrain.superkassa.presentation.theme.size.Spacing
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.cabinet.CabinetTexts

/**
 * Торговая точка: что о ней записано и что с ней можно сделать.
 *
 * Открывается справа, когда точка выбрана, а касса ещё нет. Правка живёт
 * здесь же — переименование и переезд относятся ровно к тем строкам,
 * что записаны выше, и отдельного раздела им не нужно.
 */
@Composable
fun PlaceCard(
    cabinet: CabinetWindow,
    texts: CabinetTexts,
    place: RetailPlace,
    modifier: Modifier = Modifier
) {
    val language = LocalLanguage.current
    val model = placesViewModel(cabinet.cabinet)
    val window by cabinet.cabinet.state.collectAsScreenState()
    ScrollableColumn(modifier = modifier.fillMaxWidth(), spacing = Spacing.fieldGap) {
        // Удаление стоит в конце карточки, а не в её заголовке: в узком
        // окне на заголовок приходились два значка подсказки и кнопка,
        // и «Убрать» выходило как «Убр / ать».
        SectionCard(title = texts.place, info = texts.hints.places) {
            PlaceFacts(texts, language, place)
            PlaceEditRow(cabinet, texts, place, window.busy)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            PlaceRemoval(texts, place, window.busy) { model.remove(place) }
        }
    }
}

/** Что о точке записано: название крупно, под ним адрес и число касс. */
@Composable
private fun PlaceFacts(texts: CabinetTexts, language: Language, place: RetailPlace) {
    // Две строки, а не одна: название точки владелец придумывает
    // сам, и у сети оно длинное — «Магазин «Сауда орталығы Достык
    // Плаза» отдел 12». В одну строку оно обрывалось ровно там,
    // где стоит отличие одного отдела от другого.
    Text(
        text = place.name,
        style = MaterialTheme.typography.headlineSmall,
        maxLines = TITLE_LINES,
        overflow = TextOverflow.Ellipsis
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.inline)
    ) {
        // Прочерк, а не пропуск строки: у точки без адреса строка
        // исчезала целиком, и владелец не видел, что адреса нет.
        // Прочерк — общая отметка отсутствующего значения.
        DetailLine(texts.placeAddress, addressIn(language, place.address, place.addressKz).ifBlank { Glyphs.DASH })
        DetailLine(texts.registerCount, place.cashRegisterCount.toString())
    }
}

/**
 * Удаление точки — и объяснение, когда его нет.
 *
 * Кнопка остаётся на месте и погашенной: убранная целиком, она оставляла
 * в карточке один значок подсказки, и владелец не видел, что точку вообще
 * можно удалить. Рядом с погашенной стоит причина — привязанная касса, —
 * и обе видны разом.
 */
@Composable
private fun PlaceRemoval(texts: CabinetTexts, place: RetailPlace, busy: Boolean, onRemove: () -> Unit) {
    val held = place.cashRegisterCount > 0
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.buttonGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (held) InfoTip(texts.placeRemoveBlocked)
        OutlinedButton(enabled = !held && !busy, onClick = onRemove) { Text(texts.remove) }
    }
}

/** Сколько строк отводится названию в заголовке карточки. */
private const val TITLE_LINES = 2
