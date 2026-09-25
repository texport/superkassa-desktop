package kz.mybrain.superkassa.kassa

import kz.mybrain.superkassa.MapScene
import kz.mybrain.superkassa.domain.analytics.port.Analytics
import kz.mybrain.superkassa.presentation.analytics.AnalyticsPorts
import kz.mybrain.superkassa.presentation.shell.AppContainer

/** Те же зависимости окна с аналитикой, которая ходит в [cabinet]; карта — без сети. */
fun AppContainer.analyzing(cabinet: Analytics): AppContainer =
    AppContainer(services, areas.copy(analytics = AnalyticsPorts(cabinet, MapScene.ports())))
