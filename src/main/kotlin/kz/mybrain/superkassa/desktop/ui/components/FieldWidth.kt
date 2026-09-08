package kz.mybrain.superkassa.desktop.ui.components

import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import kz.mybrain.superkassa.desktop.ui.theme.Sizes

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
fun Modifier.fieldWidth(label: String, width: Dp): Modifier {
    val measurer = rememberTextMeasurer()
    val style = MaterialTheme.typography.bodySmall
    val measured = with(LocalDensity.current) {
        measurer.measure(label, style).size.width.toDp()
    }
    return this.width(maxOf(width, measured + Sizes.fieldTextInset))
}
