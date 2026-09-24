package kz.mybrain.superkassa.presentation.analytics

import kz.mybrain.superkassa.domain.analytics.port.Analytics
import kz.mybrain.superkassa.presentation.common.mapview.MapPorts

/**
 * Порты области «Аналитика и карта» одним набором.
 *
 * В контейнере зависимостей у каждой области одно поле: так контейнер
 * не разрастается с каждым портом, а правки областей не пересекаются.
 *
 * @property cabinet аналитика кабинета: кассы на карте, обмен, продажи.
 * @property map службы карт и память о карте — их берёт и выбор места
 *   торговой точки в кабинете.
 */
class AnalyticsPorts(val cabinet: Analytics, val map: MapPorts)
