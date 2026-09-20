package kz.mybrain.superkassa.desktop.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Незаконченное подключение кассы.
 *
 * Подключение идёт через две службы и живую подпись: заводской номер даёт
 * узел, кассу заводит кабинет, на учёт её ставит ИСНА, и ответа ИСНА ждут
 * не минуту. Бросить на середине — обычное дело: номер получили сегодня,
 * ключ ЭЦП принесли через неделю. Поэтому пройденное запоминается на диске
 * и мастер открывается там, где его оставили.
 *
 * **Токен здесь не хранится.** Он выдаётся кабинетом на последнем шаге
 * и живёт только в памяти: это ключ, которым касса подписывает запросы,
 * и класть его в файл рядом с черновиком нельзя. Если мастер продолжают
 * назавтра, токен выдаётся заново — кабинет умеет выдать новый.
 */
class KkmSetupDraft(private val preferences: Preferences) {

    /** Заводской номер, который унесли в кабинет. */
    var factoryNumber: String? by mutableStateOf(preferences.setupValue(FACTORY))
        private set

    /** Год выпуска рядом с номером. */
    var manufactureYear: String? by mutableStateOf(preferences.setupValue(YEAR))
        private set

    /** Касса, заведённая в кабинете под этим номером. */
    var cabinetRegisterId: String? by mutableStateOf(preferences.setupValue(REGISTER))
        private set

    /** Идентификатор кассы у ОФД: по нему её заводят в узле. */
    var systemId: String? by mutableStateOf(preferences.setupValue(SYSTEM_ID))
        private set

    /**
     * Название, которое владелец дал кассе в кабинете.
     *
     * Запоминается вместе с кассой, потому что мастер бросают на середине:
     * назвали сегодня, завели в узле через неделю. Без этого касса
     * рождалась бы на узле безымянной.
     */
    var name: String? by mutableStateOf(preferences.setupValue(NAME))
        private set

    /** Запоминает выданный узлом номер: второй вызов дал бы другой. */
    fun rememberFactory(number: String, year: String) {
        factoryNumber = number
        manufactureYear = year
        preferences.setupValue(FACTORY, number)
        preferences.setupValue(YEAR, year)
    }

    /** Запоминает кассу, заведённую в кабинете, вместе с её названием. */
    fun rememberRegister(id: String, kkmId: Int, name: String? = null) {
        cabinetRegisterId = id
        systemId = kkmId.toString()
        this.name = name?.takeIf { it.isNotBlank() }
        preferences.setupValue(REGISTER, id)
        preferences.setupValue(SYSTEM_ID, kkmId.toString())
        preferences.setupValue(NAME, this.name)
    }

    /** Подключение завершено или начато заново: пройденное забывается. */
    fun clear() {
        factoryNumber = null
        manufactureYear = null
        cabinetRegisterId = null
        systemId = null
        name = null
        listOf(FACTORY, YEAR, REGISTER, SYSTEM_ID, NAME).forEach { preferences.setupValue(it, null) }
    }

    /** На каком шаге мастер откроется. */
    fun step(): SetupStep = when {
        factoryNumber == null -> SetupStep.Factory
        cabinetRegisterId == null -> SetupStep.Cabinet
        else -> SetupStep.Application
    }

    private companion object {
        const val FACTORY = "factory"
        const val YEAR = "year"
        const val REGISTER = "register"
        const val SYSTEM_ID = "system"
        const val NAME = "name"
    }
}

/**
 * Шаги подключения кассы.
 *
 * Порядок задан не удобством, а зависимостями: без номера кассу
 * не завести в кабинете, без кассы в кабинете не подать заявление,
 * без учёта в ИСНА не выдать токен, без токена не завести кассу в узле.
 */
enum class SetupStep { Factory, Cabinet, Application, Token, Admin }
