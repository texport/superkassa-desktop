package kz.mybrain.superkassa.desktop.ui.components

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs
import kz.mybrain.superkassa.desktop.ui.theme.MoneyStyle

/**
 * Поле, в которое кассир набирает сумму.
 *
 * Одно на все суммы кассы: принятые деньги, возврат, внесение и изъятие
 * набираются в разных разделах, но одинаково — моноширинным начертанием
 * денег и с разбивкой разрядов прямо по ходу набора. Без разбивки кассир
 * считал цифры глазами, чтобы отличить полторы тысячи от полутора
 * миллионов, а рядом стояла подпись с той же суммой, набранной разрядами.
 *
 * Разряды разделяются показом, а не правкой набранного: сама строка
 * остаётся такой, какой её набрали, и курсор не прыгает в конец, когда
 * кассир правит цифру в середине суммы.
 *
 * @param label подпись поля; по ней же считается наименьшая ширина.
 * @param placeholder правило ввода, пока поле пусто.
 */
@Composable
fun MoneyField(
    value: String,
    label: String,
    modifier: Modifier,
    isError: Boolean = false,
    placeholder: String? = null,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        // Разделители в саму строку не попадают: их ставит показ, а
        // хранится и разбирается то, что набрал кассир.
        onValueChange = { entered -> onValueChange(entered.filterNot { it.isSeparator() }) },
        label = { Text(label) },
        singleLine = true,
        textStyle = MoneyStyle.row,
        isError = isError,
        placeholder = placeholder?.let { { Text(it) } },
        visualTransformation = GroupedAmount,
        modifier = modifier
    )
}

private fun Char.isSeparator(): Boolean = this == ' ' || this == Glyphs.NBSP

/**
 * Показ набранной суммы разрядами.
 *
 * Сама разбивка берётся у денег [Money.grouped] — того же правила, по
 * которому разряды разделены в показанной сумме. Здесь остаётся только
 * пересчёт места курсора: показанная строка длиннее набранной ровно на
 * вставленные разделители.
 */
internal object GroupedAmount : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val shown = Money.grouped(raw)
        val places = shown.indices.filterNot { shown[it].isSeparator() }
        if (places.size != raw.length) return TransformedText(text, OffsetMapping.Identity)
        return TransformedText(AnnotatedString(shown), mappingOf(places, raw.length, shown.length))
    }

    private fun mappingOf(places: List<Int>, rawLength: Int, shownLength: Int): OffsetMapping =
        object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                places.getOrNull(offset.coerceIn(0, rawLength)) ?: shownLength

            override fun transformedToOriginal(offset: Int): Int =
                places.count { it < offset.coerceIn(0, shownLength) }
        }
}
