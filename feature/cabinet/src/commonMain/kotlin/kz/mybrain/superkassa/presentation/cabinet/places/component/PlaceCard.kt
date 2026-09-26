package kz.mybrain.superkassa.presentation.cabinet.places.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import kz.mybrain.superkassa.designsystem.list.ScrollableColumn
import kz.mybrain.superkassa.designsystem.section.DetailLine
import kz.mybrain.superkassa.designsystem.section.SectionCard
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.designsystem.theme.icon.Glyphs
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.domain.cabinet.model.RetailPlace
import kz.mybrain.superkassa.presentation.cabinet.CabinetWindow
import kz.mybrain.superkassa.presentation.cabinet.mapPointOf
import kz.mybrain.superkassa.presentation.cabinet.places.placesViewModel
import kz.mybrain.superkassa.presentation.common.mapview.MapPoint
import kz.mybrain.superkassa.presentation.common.model.collectAsScreenState
import kz.mybrain.superkassa.presentation.words.cabinet.addressIn
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.cabinet.CabinetTexts

/**
 * Торговая точка: что о ней записано и что с ней можно сделать.
 *
 * Открывается справа, когда точка выбрана, а касса ещё нет. Сверху —
 * название и три строки сведений: адрес, число касс и место на карте;
 * под ними — ряд действий: переименовать, сменить адрес, удалить.
 * Правка открывается окном по кнопке: прежде поля названия, подбора
 * адреса, координаты и три кнопки стояли в карточке подряд, и за ними
 * не было видно, что о точке уже записано и что к чему относится.
 */
@Composable
internal fun PlaceCard(
    cabinet: CabinetWindow,
    texts: CabinetTexts,
    place: RetailPlace,
    modifier: Modifier = Modifier
) {
    val language = LocalLanguage.current
    val model = placesViewModel(cabinet.cabinet)
    val window by cabinet.cabinet.state.collectAsScreenState()
    ScrollableColumn(modifier = modifier.fillMaxWidth(), spacing = Spacing.fieldGap) {
        SectionCard(title = texts.places.place, info = texts.hints.places) {
            PlaceFacts(texts, language, place)
            PlaceEditRow(cabinet, texts, place, window.busy) { model.remove(place) }
        }
    }
}

/** Что о точке записано: название крупно, под ним адрес, число касс и место на карте. */
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
        DetailLine(texts.places.address, addressIn(language, place.address, place.addressKz).ifBlank { Glyphs.DASH })
        DetailLine(texts.places.registerCount, place.cashRegisterCount.toString())
        DetailLine(texts.places.point, pointLine(texts, mapPointOf(place.latitude, place.longitude)))
    }
}

/** Место на карте одной строкой: широта и долгота через запятую — или что его нет. */
private fun pointLine(texts: CabinetTexts, point: MapPoint?): String =
    point?.let { "${it.latitude}, ${it.longitude}" } ?: texts.places.pointNotChosen

/** Сколько строк отводится названию в заголовке карточки. */
private const val TITLE_LINES = 2
