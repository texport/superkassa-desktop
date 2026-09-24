package kz.mybrain.superkassa.presentation.shell.frame

import kz.mybrain.superkassa.presentation.analytics.AnalyticsScreen
import kz.mybrain.superkassa.presentation.cabinet.CabinetNeighbours
import kz.mybrain.superkassa.presentation.map.MapPointPicker
import kz.mybrain.superkassa.presentation.shell.AppContainer
import kz.mybrain.superkassa.presentation.users.signin.loginViewModel

/**
 * Соседи кабинета в этом окне: аналитика, окно карты, память мастера и вход.
 *
 * Собираются здесь, в каркасе: каркас видит все области, а кабинет —
 * ни одной, и получает от них только готовое.
 */
fun cabinetNeighbours(app: AppContainer): CabinetNeighbours = CabinetNeighbours(
    analytics = { access, texts -> AnalyticsScreen(app, access, texts) },
    points = MapPointPicker(app.areas.analytics.map),
    setupMemory = app.areas.setup?.memory,
    kkmsReload = { loginViewModel(app)::reload }
)
