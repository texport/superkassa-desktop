package kz.mybrain.superkassa.presentation.analytics

import kz.mybrain.superkassa.domain.analytics.port.Analytics
import kz.mybrain.superkassa.domain.analytics.port.FakeAnalytics
import kz.mybrain.superkassa.domain.map.MemoryMapMemory
import kz.mybrain.superkassa.domain.map.QuietMaps
import kz.mybrain.superkassa.domain.map.port.Maps
import kz.mybrain.superkassa.presentation.analytics.map.AnalyticsMapViewModel
import kz.mybrain.superkassa.presentation.analytics.map.KkmMapCases
import kz.mybrain.superkassa.presentation.common.mapview.MapPorts
import kz.mybrain.superkassa.presentation.shell.AppContainer

/** Порты аналитики для проверок: кабинет по заказу, карта без сети, память в процессе. */
fun analyticsPorts(cabinet: Analytics = FakeAnalytics(), maps: Maps = QuietMaps()) =
    AnalyticsPorts(cabinet, MapPorts(maps, MemoryMapMemory()))

/** Те же зависимости окна с аналитикой, которая ходит в [cabinet]. */
fun AppContainer.analyzing(cabinet: Analytics): AppContainer =
    AppContainer(kassa, signIn, memory, look, talk, areas.copy(analytics = analyticsPorts(cabinet)))

/** Модель карты касс на подставном кабинете и службах карт; память карты — своя у каждой проверки. */
fun mapModel(cabinet: Analytics, maps: Maps = QuietMaps()): AnalyticsMapViewModel =
    AnalyticsMapViewModel(KkmMapCases(cabinet, MapPorts(maps, MemoryMapMemory()).cases()))
