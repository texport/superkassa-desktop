package kz.mybrain.superkassa.presentation.common.mapview.control

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.theme.size.Sizes
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.strings.api.map.MapTexts

/**
 * Строки в левом нижнем углу карты, снизу вверх: чья это карта и, если ни
 * одна плитка не доехала, почему поле пустое.
 *
 * Правый нижний угол занят легендой карты касс, а подпись авторства
 * поставщика обязана быть видна — поэтому она слева, у самого края. До
 * первой неудачи поле не объясняется: жаловаться ещё не на что.
 */
@Composable
internal fun MapNotes(blank: Boolean, attribution: String, texts: MapTexts, modifier: Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.itemGap)) {
        if (blank) MapNote(texts.noTiles)
        MapNote(attribution)
    }
}

/**
 * Строка поверх карты: почему поле пустое, или чья это карта.
 *
 * Стоит в нижнем углу, а не в середине: середину занимает метка, и ради
 * объяснения закрывать её нельзя. Заливка поверхности с тенью — иначе
 * надпись теряется на подложке там, где плитки всё-таки доехали.
 * Подпись авторства — всегда на виду: её требуют условия поставщика плиток.
 */
@Composable
private fun MapNote(notice: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(Sizes.corner),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = Sizes.mapMarkLift
    ) {
        Text(
            text = notice,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.fieldGap, vertical = Spacing.itemGap)
        )
    }
}
