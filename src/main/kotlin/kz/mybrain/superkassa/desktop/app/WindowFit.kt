package kz.mybrain.superkassa.desktop.app

import java.awt.GraphicsEnvironment
import java.awt.Toolkit

/**
 * Размер окна кассы, помещающийся на экране этой машины.
 *
 * Окно помнит размер с прошлого запуска, а экран у него не всегда тот же:
 * рабочее место разворачивают на внешнем мониторе, а утром касса
 * открывается на ноутбуке. Окно шире экрана уезжает краем за него вместе
 * с кнопками шапки, и кассиру остаётся тянуть его мышью обратно.
 *
 * Ужимается, но не растягивается: маленькое окно кассир выбрал сам.
 */
internal fun fitToScreen(wanted: Pair<Int, Int>, screen: Pair<Int, Int>?): Pair<Int, Int> {
    val (width, height) = wanted
    val (screenWidth, screenHeight) = screen ?: return wanted
    return minOf(width, screenWidth) to minOf(height, screenHeight)
}

/**
 * Рабочая площадь экрана без панели задач и дока.
 *
 * Меряется в тех же точках, которыми задаётся окно: система на экранах
 * с удвоенной плотностью отдаёт их уже пересчитанными, и второй раз
 * делить не на что. Экрана может не оказаться вовсе — касса собирается
 * и проверяется без него, — и тогда ужимать не под что.
 */
internal fun screenSize(): Pair<Int, Int>? = runCatching {
    if (GraphicsEnvironment.isHeadless()) return null
    val screen = GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration
    val bounds = screen.bounds
    val insets = Toolkit.getDefaultToolkit().getScreenInsets(screen)
    (bounds.width - insets.left - insets.right) to (bounds.height - insets.top - insets.bottom)
}.getOrNull()
