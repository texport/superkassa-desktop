package kz.mybrain.superkassa.presentation.cabinet.places.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import kz.mybrain.superkassa.domain.cabinet.model.RetailPlace
import kz.mybrain.superkassa.presentation.cabinet.addressIn
import kz.mybrain.superkassa.presentation.strings.cabinet.CabinetTexts
import kz.mybrain.superkassa.presentation.strings.common.Language
import kz.mybrain.superkassa.presentation.theme.size.Spacing

/**
 * Что стоит под названием торговой точки в колонке: адрес и число касс.
 *
 * Адрес — то, чем точка и опознаётся: в колонке стояло только «Касс: 3»,
 * и три магазина одной сети отличались друг от друга лишь названием,
 * которое владелец сам же и придумал. Адрес идёт первым, число касс под ним
 * служебной шкалой.
 *
 * Адреса нет у точки, которую только что создали, — тогда и строки нет,
 * а не пустое место под названием.
 */
@Composable
internal fun PointSupport(texts: CabinetTexts, language: Language, place: RetailPlace) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.hairline)) {
        val address = addressIn(language, place.address, place.addressKz)
        if (address.isNotBlank()) {
            SupportLine(address, MaterialTheme.typography.bodySmall, ADDRESS_LINES)
        }
        SupportLine("${texts.registerCount}: ${place.cashRegisterCount}", MaterialTheme.typography.labelSmall)
    }
}

/** Служебная строка под названием: приглушённая и без выхода за край колонки. */
@Composable
private fun SupportLine(text: String, style: TextStyle, lines: Int = 1) {
    Text(
        text = text,
        style = style,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = lines,
        overflow = TextOverflow.Ellipsis
    )
}

/** Сколько строк отводится адресу: улица с домом в узкую колонку не встаёт. */
private const val ADDRESS_LINES = 2
