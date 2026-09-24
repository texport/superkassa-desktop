package kz.mybrain.superkassa.domain.kassa.port

/**
 * Что продаже и возврату нужно снаружи сверх кассы: настройки доставки чека.
 *
 * Собирается в точке сборки платформы; настройки ядро отдаёт отдельным
 * от кассовых операций фасадом.
 */
class KassaPorts(val delivery: DeliverySetup)
