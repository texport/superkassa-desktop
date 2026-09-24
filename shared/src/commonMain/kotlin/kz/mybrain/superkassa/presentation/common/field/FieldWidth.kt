package kz.mybrain.superkassa.presentation.common.field

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import kz.mybrain.superkassa.presentation.theme.size.Sizes

/**
 * Ширина поля ввода: не уже собственной подписи.
 *
 * Подпись поля Material переносит на вторую строку, когда не помещается,
 * и поле становится выше соседних — ряд разъезжается. По-казахски подписи
 * длиннее русских на треть, и по числу из токена они не помещались.
 *
 * Ширина считается так же, как у сегментов и у рельса разделов: по самой
 * подписи плюс поля рамки. Правило одно на весь интерфейс, а не подбор
 * числа на каждом экране.
 *
 * @param label подпись поля — та же строка, что стоит в `label`.
 * @param width желаемая ширина; берётся, пока подпись в неё помещается.
 */
@Composable
fun Modifier.fieldWidth(label: String, width: Dp): Modifier = this.width(labelledWidth(label, width))

/**
 * Поле формы, которое тянется до конца строки: не уже [width] и собственной
 * подписи, а шире — сколько даёт строка.
 *
 * Для полей в переносимом ряду формы: с `Modifier.weight(1f)` перед ним поле
 * занимает остаток строки, а когда не помещается и наименьшим — уходит
 * на следующую целиком. В широкой карточке поле фиксированной ширины
 * оставляло справа от себя пустую половину строки.
 */
@Composable
fun Modifier.fieldMinWidth(label: String, width: Dp): Modifier = this.widthIn(min = labelledWidth(label, width))

@Composable
private fun labelledWidth(label: String, width: Dp): Dp {
    val measurer = rememberTextMeasurer()
    val style = MaterialTheme.typography.bodySmall
    val density = LocalDensity.current
    // Подпись меряется один раз: при растягивании окна разметка
    // пересчитывается десятки раз в секунду, а подпись от ширины окна
    // не зависит.
    val measured = remember(label, style, density) {
        with(density) { measurer.measure(label, style).size.width.toDp() }
    }
    return maxOf(width, measured + Sizes.fieldTextInset)
}
