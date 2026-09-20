package kz.mybrain.superkassa.desktop.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.server.DictionaryEntry
import kz.mybrain.superkassa.desktop.ui.strings.EnumStrings
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.paymentFallback
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Выбор вида оплаты — выпадающим списком.
 *
 * Шесть плашек занимали две-три строки в самом низу кассовой колонки,
 * а не хватало этой высоты вводу товара: карточка позиции сжималась так,
 * что цену и ставку приходилось доставать прокруткой внутри карточки.
 * Список отдаёт высоту вводу, а выбранный вид виден в поле и без
 * открывания.
 *
 * Допустимость вида объявляет узел полем `supported`: непринимаемый вид
 * не прячется, а гаснет в списке и объясняется строкой под полем.
 * Спрятать его значило бы разойтись с узлом молча.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentPicker(
    entries: List<DictionaryEntry>,
    language: String,
    selectedCode: String,
    onSelect: (String) -> Unit,
    unsupportedNote: String = "",
    modifier: Modifier = Modifier
) {
    val texts = LocalStrings.current
    val refused = entries.filterNot { it.supported }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.tight)
    ) {
        LabelledPicker(
            label = texts.sale.payment,
            options = entries,
            selected = entries.firstOrNull { it.code == selectedCode },
            title = { entry -> paymentTitle(entries, language, entry?.code.orEmpty(), texts.enums) },
            onSelect = { onSelect(it.code) },
            available = { it.supported }
        )
        // Погасший вид сам по себе не объясняет, почему он погас: причина
        // одна на все непринимаемые виды и пишется один раз.
        if (refused.isNotEmpty() && unsupportedNote.isNotBlank()) {
            Text(
                text = unsupportedNote,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Название вида оплаты: сначала от узла, потом своё. */
private fun paymentTitle(
    entries: List<DictionaryEntry>,
    language: String,
    code: String,
    texts: EnumStrings
): String {
    val fromNode = entries.firstOrNull { it.code == code }?.title(language)
    return fromNode?.takeIf { it != code } ?: texts.paymentFallback(code) ?: code
}
