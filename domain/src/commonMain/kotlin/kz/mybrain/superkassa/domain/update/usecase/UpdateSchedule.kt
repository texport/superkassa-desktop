package kz.mybrain.superkassa.domain.update.usecase

import kz.mybrain.superkassa.domain.update.port.UpdateMemory
import kz.mybrain.superkassa.domain.version.model.AppVersion
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/**
 * Расписание проверки выпусков, как его помнит рабочее место.
 *
 * Раз в сутки: выпуски выходят реже, а служба считает обращения. Срок
 * считается от записанной проверки, а не от запуска: касса, включаемая
 * каждое утро, спрашивает службу раз в день, а не при каждом включении.
 */
class ReadUpdateSchedule(
    private val memory: UpdateMemory,
    private val installed: AppVersion,
    private val now: () -> Instant
) {

    /**
     * @property automatic проверять ли выпуски самой.
     * @property lastChecked когда выпуски проверялись в последний раз.
     * @property due пора ли проверять по расписанию; сборку разработчика
     *   не проверяют никогда — выпуски ей не предлагаются.
     */
    data class Schedule(val automatic: Boolean, val lastChecked: Instant?, val due: Boolean)

    operator fun invoke(): Schedule {
        val last = memory.lastChecked
        val due = memory.automatic && !installed.development && (last == null || now() - last >= BETWEEN_CHECKS)
        return Schedule(memory.automatic, last, due)
    }

    private companion object {
        val BETWEEN_CHECKS = 1.days
    }
}

/** Выключенная проверка помнится рабочим местом: на месте без интернета она только пишет отказы. */
class SwitchAutomaticChecks(private val memory: UpdateMemory) {

    operator fun invoke(on: Boolean) {
        memory.automatic = on
    }
}
