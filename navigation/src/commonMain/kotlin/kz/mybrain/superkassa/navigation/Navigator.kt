package kz.mybrain.superkassa.navigation

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavKey

/**
 * Переход по ключу экрана — то, чем экраны пользуются вместо ссылок
 * друг на друга.
 *
 * История «назад» одна на окно, и держит её каркас; экран только просит
 * открыть ключ или шагнуть назад. Так область открывает соседку, не видя
 * её, а жест Android, Escape и стрелка в шапке ведут по одной истории.
 */
interface Navigator {
    /** Открыть экран [key] поверх открытого. */
    fun open(key: NavKey)

    /** Шаг назад по истории окна. */
    fun back()
}

/**
 * Переход окна для экранов под ним.
 *
 * Без каркаса — в превью и снимках вида — переходов нет: нажатие ничего
 * не открывает, и экрану не нужно проверять, есть ли куда идти.
 */
val LocalNavigator = staticCompositionLocalOf<Navigator> { Nowhere }

/** Переходов нет: экран нарисован без окна. */
private object Nowhere : Navigator {
    override fun open(key: NavKey) = Unit

    override fun back() = Unit
}
