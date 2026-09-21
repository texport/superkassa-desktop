package kz.mybrain.superkassa.desktop.ui.components

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.ui.theme.Sizes

/**
 * Прокрутка с клавиатуры — одна на все длинные столбцы и списки.
 *
 * Колесо мыши было единственным способом доехать до нижних карточек:
 * ни PageDown, ни стрелки столбец не слушал, и до режима отладки и сведений
 * об узле в настройках нельзя было добраться ни клавиатурой, ни средствами
 * доступности. Настольное приложение обязано работать с клавиатуры целиком.
 *
 * Нажатие берётся на всплытии, а не на просмотре: стрелка внутри поля ввода
 * или списка принадлежит ему, и перехваченная сверху она увела бы страницу
 * вместо курсора. Столбец получает своё только тогда, когда содержимое
 * нажатие не взяло.
 *
 * Сам столбец при этом становится точкой обхода: без неё клавиатуре
 * не за что зацепиться на экране, где нет ни одного поля.
 *
 * @param state чем прокручивается содержимое.
 * @param viewport высота видимой части в точках: ею меряется шаг страницы.
 */
@Composable
fun Modifier.scrolledByKeys(state: ScrollableState, viewport: () -> Int): Modifier {
    val scope = rememberCoroutineScope()
    val line = with(LocalDensity.current) { Sizes.scrollLine.toPx() }
    return onKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
        val by = scrollBy(event.key, viewport(), line) ?: return@onKeyEvent false
        scope.launch { state.animateScrollBy(by) }
        true
    }.focusable()
}

/**
 * На сколько точек двигает содержимое эта клавиша; `null` — не двигает.
 *
 * Страница короче видимой части на одну строку: после прыжка на экране
 * остаётся строка от прежнего вида, и читающий не теряет место.
 */
private fun scrollBy(key: Key, viewport: Int, line: Float): Float? {
    val page = (viewport - line).coerceAtLeast(line)
    return when (key) {
        Key.PageDown -> page
        Key.PageUp -> -page
        Key.DirectionDown -> line
        Key.DirectionUp -> -line
        else -> null
    }
}
