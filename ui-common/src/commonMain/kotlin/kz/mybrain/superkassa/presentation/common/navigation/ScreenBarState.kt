package kz.mybrain.superkassa.presentation.common.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Заголовок шапки окна, заданный открытым экраном.
 *
 * Шапка у окна одна (Material 3: один `TopAppBar`), и называет она то,
 * что открыто: по умолчанию — раздел окна, а когда внутри раздела открыт
 * шаг — например, раздел настроек поверх их списка, — сам этот шаг.
 * Экран своей шапки не строит: он только сообщает каркасу, как себя
 * назвать, а каркас рисует шапку и стрелку назад по истории окна.
 */
class ScreenBarState {
    private var owner: Any? by mutableStateOf(null)

    /** Заголовок открытого шага; `null` — шапка называет раздел. */
    var title: String? by mutableStateOf(null)
        private set

    /** Строка под заголовком: чьё это; `null` — касса и кассир, как всегда. */
    var subtitle: String? by mutableStateOf(null)
        private set

    internal fun set(by: Any, title: String, subtitle: String?) {
        owner = by
        this.title = title
        this.subtitle = subtitle
    }

    /** Снять заголовок — только свой: соседний шаг мог успеть задать свой. */
    internal fun clear(by: Any) {
        if (owner != by) return
        owner = null
        title = null
        subtitle = null
    }
}

/** Заголовок шапки окна для экранов под ней; без каркаса его некому показать. */
val LocalScreenBar = staticCompositionLocalOf { ScreenBarState() }

/**
 * Назвать открытый шаг в шапке окна, пока он на экране.
 *
 * @param subtitle чьё это: например, «Настройки кассы «Касса у входа»».
 */
@Composable
fun ScreenBar(title: String, subtitle: String? = null) {
    val bar = LocalScreenBar.current
    val me = remember { Any() }
    DisposableEffect(bar, title, subtitle) {
        bar.set(me, title, subtitle)
        onDispose { bar.clear(me) }
    }
}
