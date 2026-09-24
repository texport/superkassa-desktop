package kz.mybrain.superkassa.presentation.analytics.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.mybrain.superkassa.presentation.analytics.AnalyticsPorts

/**
 * Модель карты касс: одна на окно.
 *
 * Живёт в хранилище моделей окна и переживает уход владельца в другой
 * раздел — вернувшись, он видит прочитанное, а не ждёт его заново.
 */
@Composable
fun analyticsMapViewModel(ports: AnalyticsPorts): AnalyticsMapViewModel = viewModel { analyticsMapModel(ports) }

/** Модель карты касс без окна — для проверок и для окна. */
fun analyticsMapModel(ports: AnalyticsPorts): AnalyticsMapViewModel =
    AnalyticsMapViewModel(KkmMapCases(ports.cabinet, ports.map.cases()))

/** Плитки, определение места, карточка и легенда карты касс — на время окна. */
@Composable
fun mapTools(ports: AnalyticsPorts): MapTools = remember(ports) {
    val map = ports.map.cases()
    MapTools(tiles = map.tiles(), locating = map.locating(), panel = map.card(), legend = map.legend())
}
