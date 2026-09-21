package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kz.mybrain.superkassa.desktop.app.Preferences
import kz.mybrain.superkassa.desktop.server.DictionaryEntry
import kz.mybrain.superkassa.desktop.ui.components.OfdTarget
import kz.mybrain.superkassa.desktop.ui.components.environmentRaised

/**
 * Ход заведения кассы на этой машине.
 *
 * Выпущенный токен держится здесь до конца хода. Кабинет умеет только
 * выпустить новый — показать действующий он не умеет, — и второй выпуск
 * после неудачного заведения обесценил бы первый: касса осталась бы
 * с токеном, которого не знает ни одна машина.
 *
 * Контур запоминается рабочим местом: на одной машине он не меняется,
 * и выбирать его заново при каждой кассе владельцу незачем.
 */
class AdoptDraft(private val preferences: Preferences) {

    /** Куда касса будет слать чеки: БФД и запомненный контур. */
    var target by mutableStateOf(OfdTarget(environment = preferences.setupValue(ENVIRONMENT).orEmpty()))

    /** Пин будущего администратора кассы: набирает владелец, нигде не хранится. */
    var adminPin by mutableStateOf("")

    /** Отметка о том, что касса на другой машине замолчит. */
    var handoverAccepted by mutableStateOf(false)

    /** Токен, уже выпущенный в этом ходе. */
    var issued: String? by mutableStateOf(null)
        private set

    /** Токен выпущен, а касса не заведена: прежний токен уже недействителен. */
    var stranded by mutableStateOf(false)
        private set

    /** Выпущенный токен остаётся на второй заход — он же и уйдёт. */
    fun keepToken(token: String) {
        issued = token
    }

    /** Заведение не удалось; отказ важен только когда токен уже выпущен. */
    fun strand() {
        stranded = issued != null
    }

    /** Владелец начал новую попытку — прежний отказ снят с экрана. */
    fun retrying() {
        stranded = false
    }

    /** Незаполненный контур берёт прошлое значение, а в первый раз — первый поднятый. */
    fun preset(environments: List<DictionaryEntry>) {
        target = target.copy(
            environment = target.environment.ifBlank {
                environments.firstOrNull { it.environmentRaised() }?.code.orEmpty()
            }
        )
    }

    /** Запоминает выбор владельца — но только после удавшегося заведения. */
    fun remember() {
        preferences.setupValue(ENVIRONMENT, target.environment)
    }

    /** Заполненное владельцем в том виде, в каком его читают правила. */
    fun form(handoverNeeded: Boolean) =
        AdoptForm(target.complete, adminPin, handoverNeeded, handoverAccepted)

    private companion object {
        const val ENVIRONMENT = "environment"
    }
}
