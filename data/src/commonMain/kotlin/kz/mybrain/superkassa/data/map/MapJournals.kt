package kz.mybrain.superkassa.data.map

import kz.mybrain.superkassa.domain.log.port.Journal
import kz.mybrain.superkassa.integrations.maps.MapJournal

/**
 * Неудачи служб карты — в журнал приложения.
 *
 * Строка называет службу и имя помехи, без адреса и координат: адреса
 * торговых точек владельца в файл для поддержки не уходят.
 */
fun mapJournal(journal: Journal): MapJournal = MapJournal { service, reason -> journal.warn("map $service: $reason") }
