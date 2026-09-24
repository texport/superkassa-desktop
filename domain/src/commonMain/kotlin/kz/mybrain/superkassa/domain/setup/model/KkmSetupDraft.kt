package kz.mybrain.superkassa.domain.setup.model

import kz.mybrain.superkassa.domain.setup.port.SetupMemory

/**
 * Незаконченное подключение кассы.
 *
 * Подключение идёт через две службы и живую подпись: заводской номер даёт
 * касса, в кабинете её заводит владелец, на учёт её ставит ИСНА, и ответа
 * ИСНА ждут не минуту. Бросить на середине — обычное дело: номер получили
 * сегодня, ключ ЭЦП принесли через неделю. Поэтому пройденное запоминается
 * на диске, и мастер открывается там, где его оставили.
 *
 * **Токена здесь нет.** Он выдаётся кабинетом на последнем шаге и живёт
 * только внутри одного действия: это ключ, которым касса подписывает
 * запросы. Если мастер продолжают назавтра, кабинет выдаёт новый.
 *
 * @property factoryNumber заводской номер, который унесли в кабинет.
 * @property cabinetRegisterId касса, заведённая в кабинете под этим номером.
 * @property systemId идентификатор кассы у БФД: по нему её заводят здесь.
 * @property name название, данное кассе в кабинете: назвали сегодня, завели
 *   через неделю — без этого касса рождалась бы безымянной.
 */
data class KkmSetupDraft(
    val factoryNumber: String? = null,
    val manufactureYear: String? = null,
    val cabinetRegisterId: String? = null,
    val systemId: String? = null,
    val name: String? = null
) {
    /** На каком шаге мастер откроется. */
    val step: SetupStep
        get() = when {
            factoryNumber == null -> SetupStep.Factory
            cabinetRegisterId == null -> SetupStep.Cabinet
            else -> SetupStep.Application
        }

    /** Записывает пройденное целиком; пустое поле стирает запись. */
    fun saveTo(memory: SetupMemory) {
        memory.setupValue(FACTORY, factoryNumber)
        memory.setupValue(YEAR, manufactureYear)
        memory.setupValue(REGISTER, cabinetRegisterId)
        memory.setupValue(SYSTEM_ID, systemId)
        memory.setupValue(NAME, name)
    }

    companion object {
        /** Пройденное, каким его оставили в прошлый раз. */
        fun readFrom(memory: SetupMemory): KkmSetupDraft = KkmSetupDraft(
            factoryNumber = memory.setupValue(FACTORY),
            manufactureYear = memory.setupValue(YEAR),
            cabinetRegisterId = memory.setupValue(REGISTER),
            systemId = memory.setupValue(SYSTEM_ID),
            name = memory.setupValue(NAME)
        )

        private const val FACTORY = "factory"
        private const val YEAR = "year"
        private const val REGISTER = "register"
        private const val SYSTEM_ID = "system"
        private const val NAME = "name"
    }
}

/**
 * Шаги подключения кассы.
 *
 * Порядок задан не удобством, а зависимостями: без номера кассу
 * не завести в кабинете, без кассы в кабинете не подать заявление,
 * без учёта в ИСНА не выдать токен, без токена не завести кассу здесь.
 */
enum class SetupStep { Factory, Cabinet, Application, Token, Admin }
