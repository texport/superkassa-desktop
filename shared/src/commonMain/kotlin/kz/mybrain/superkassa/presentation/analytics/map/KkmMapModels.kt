package kz.mybrain.superkassa.presentation.analytics.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.mybrain.superkassa.presentation.shell.AppContainer

/**
 * Модель карты касс: одна на окно.
 *
 * Живёт в хранилище моделей окна и переживает уход владельца в другой
 * раздел — вернувшись, он видит прочитанное, а не ждёт его заново.
 */
@Composable
fun analyticsMapViewModel(app: AppContainer): AnalyticsMapViewModel = viewModel { analyticsMapModel(app) }

/** Модель карты касс без окна — для проверок и для окна. */
fun analyticsMapModel(app: AppContainer): AnalyticsMapViewModel =
    AnalyticsMapViewModel(KkmMapCases(app.areas.analytics.cabinet, app.areas.analytics.map.cases()))

/** Плитки, определение места, карточка и легенда карты касс — на время окна. */
@Composable
fun mapTools(app: AppContainer): MapTools = remember(app) {
    val map = app.areas.analytics.map.cases()
    MapTools(tiles = map.tiles(), locating = map.locating(), panel = map.card(), legend = map.legend())
}
