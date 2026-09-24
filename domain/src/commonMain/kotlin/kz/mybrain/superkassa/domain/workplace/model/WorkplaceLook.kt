package kz.mybrain.superkassa.domain.workplace.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kz.mybrain.superkassa.domain.workplace.port.LookMemory

/**
 * Вид рабочего места: одно на приложение место, где записан выбор кассира.
 *
 * Выбор держится состоянием, а не читается из памяти при каждом обращении:
 * память экран не наблюдает, и сменённый язык оставался бы на экране
 * прежним до перезапуска. Окно, строка сообщений и настройки видят одно
 * и то же состояние.
 */
class WorkplaceLook(private val memory: LookMemory) {
    private val current = MutableStateFlow(memory.look)

    val state: StateFlow<LookChoice> = current.asStateFlow()

    /** Запоминает новый выбор и сразу показывает его всем. */
    fun choose(chosen: LookChoice) {
        if (chosen == current.value) return
        memory.look = chosen
        current.value = chosen
    }

    /**
     * Перечитывает выбор из памяти: его могли сменить в обход кассы —
     * на Android язык приложения выбирают и в настройках системы.
     */
    fun recall() {
        current.value = memory.look
    }
}
