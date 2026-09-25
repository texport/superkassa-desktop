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
 * @property way путь, выбранный владельцем; `null` — ещё не выбран.
 */
data class KkmSetupDraft(
    val factoryNumber: String? = null,
    val manufactureYear: String? = null,
    val cabinetRegisterId: String? = null,
    val systemId: String? = null,
    val name: String? = null,
    val way: SetupWay? = null
) {
    /**
     * Шаг [step] пройден по записанному.
     *
     * Учёт в КГД знает только кабинет, идентификатор и токен ручного пути
     * не запоминаются вовсе — токен на диск не пишется, — поэтому эти шаги
     * пройденными не считаются никогда, и мастер продолжают с них.
     */
    fun passed(step: SetupStep): Boolean = when (step) {
        SetupStep.Way -> way != null
        SetupStep.Factory -> factoryNumber != null
        SetupStep.Cabinet -> cabinetRegisterId != null
        else -> false
    }

    /** Записывает пройденное целиком; пустое поле стирает запись. */
    fun saveTo(memory: SetupMemory) {
        memory.setupValue(FACTORY, factoryNumber)
        memory.setupValue(YEAR, manufactureYear)
        memory.setupValue(REGISTER, cabinetRegisterId)
        memory.setupValue(SYSTEM_ID, systemId)
        memory.setupValue(NAME, name)
        memory.setupValue(WAY, way?.name)
    }

    companion object {
        /** Пройденное, каким его оставили в прошлый раз. */
        fun readFrom(memory: SetupMemory): KkmSetupDraft = KkmSetupDraft(
            factoryNumber = memory.setupValue(FACTORY),
            manufactureYear = memory.setupValue(YEAR),
            cabinetRegisterId = memory.setupValue(REGISTER),
            systemId = memory.setupValue(SYSTEM_ID),
            name = memory.setupValue(NAME),
            way = memory.setupValue(WAY)?.let { saved -> SetupWay.entries.firstOrNull { it.name == saved } }
        )

        private const val FACTORY = "factory"
        private const val YEAR = "year"
        private const val REGISTER = "register"
        private const val SYSTEM_ID = "system"
        private const val NAME = "name"
        private const val WAY = "way"
    }
}
