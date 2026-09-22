package kz.mybrain.superkassa.desktop.ui.map

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kz.mybrain.superkassa.desktop.ui.theme.Durations

/**
 * Плавный переход карты к названной цели.
 *
 * Живёт внутри показа карты, а не в её состоянии: ход кадров есть только
 * у композиции, и состояние, запускающее сопрограмму само, пришлось бы
 * снабдить своим сроком жизни и своим потоком.
 *
 * Путь считается в точках полотна, а не в градусах: по широте градус
 * меняет длину с удалением от экватора, и переход, ровный в градусах,
 * на карте замедлялся бы к середине.
 *
 * Цель снимается по прибытии — и снимается ею же, если владелец потянул
 * карту рукой: начатый переход не должен уводить её обратно.
 */
@Composable
internal fun MapGlide(state: MapState) {
    val goal = state.goal ?: return
    LaunchedEffect(goal) {
        val zoom = state.zoom
        val fromX = MapProjection.xOf(state.centerLongitude, zoom)
        val fromY = MapProjection.yOf(state.centerLatitude, zoom)
        val toX = MapProjection.xOf(goal.longitude, zoom)
        val toY = MapProjection.yOf(goal.latitude, zoom)
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = tween(Durations.mapGlide.inWholeMilliseconds.toInt(), easing = FastOutSlowInEasing)
        ) { share, _ ->
            // Переход прерван рукой владельца: цели больше нет, и вести
            // карту дальше значило бы спорить с ним.
            if (state.goal != goal) return@animate
            state.centreOn(
                latitude = MapProjection.latitudeOf(fromY + (toY - fromY) * share, zoom),
                longitude = MapProjection.longitudeOf(fromX + (toX - fromX) * share, zoom)
            )
        }
        if (state.goal == goal) state.arrived()
    }
}
