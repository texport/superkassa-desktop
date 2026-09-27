package kz.mybrain.superkassa.data.log

import org.slf4j.ILoggerFactory
import org.slf4j.IMarkerFactory
import org.slf4j.helpers.BasicMarkerFactory
import org.slf4j.helpers.NOPMDCAdapter
import org.slf4j.spi.MDCAdapter
import org.slf4j.spi.SLF4JServiceProvider

/**
 * Журнал ядра в журнале рабочего места.
 *
 * Ядро пишет через SLF4J: что ушло в БФД, с каким кодом тот ответил,
 * почему пакет не разобран. Без привязки SLF4J в приложении всё это
 * уходило в никуда, и отказ X-отчёта в журнале выглядел пустым местом.
 * SLF4J находит этот класс сам — по файлу службы в `META-INF/services`.
 */
class CoreLogProvider : SLF4JServiceProvider {

    private val loggers = ILoggerFactory { name -> CoreLogger(name) }

    private val markers = BasicMarkerFactory()

    private val context = NOPMDCAdapter()

    override fun getLoggerFactory(): ILoggerFactory = loggers

    override fun getMarkerFactory(): IMarkerFactory = markers

    override fun getMDCAdapter(): MDCAdapter = context

    override fun getRequestedApiVersion(): String = API_VERSION

    override fun initialize() = Unit

    private companion object {
        /** Версия SLF4J, под которую написана привязка. */
        const val API_VERSION = "2.0.99"
    }
}
