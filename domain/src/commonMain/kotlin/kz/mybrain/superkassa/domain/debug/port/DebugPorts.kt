package kz.mybrain.superkassa.domain.debug.port

/**
 * Что отладке нужно снаружи: журнал рабочего места.
 *
 * @property logBook журнал рабочего места для окна отладки и режима отладки.
 */
data class DebugPorts(val logBook: LogBook)
