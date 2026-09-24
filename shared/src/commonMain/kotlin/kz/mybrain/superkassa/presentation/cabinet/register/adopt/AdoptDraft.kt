package kz.mybrain.superkassa.presentation.cabinet.register.adopt

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.texport.superkassa.core.presentation.api.model.reference.OfdEnvironmentResponse
import kz.mybrain.superkassa.domain.setup.model.OfdContours
import kz.mybrain.superkassa.domain.setup.port.SetupMemory
import kz.mybrain.superkassa.presentation.cabinet.register.AdoptForm

/**
 * Заполняемое владельцем при заведении кассы на этой машине.
 *
 * Выпущенный токен держит модель заведения, а не форма: закрытое и снова
 * открытое окно не должно выпускать второй токен, обесценивая первый.
 *
 * Контур запоминается рабочим местом: на одной машине он не меняется,
 * и выбирать его заново при каждой кассе владельцу незачем.
 *
 * @param memory память мастера подключения; `null` — на этой платформе
 *   мастера нет, и контур выбирается каждый раз.
 */
class AdoptDraft(private val memory: SetupMemory?) {

    /** Куда касса будет слать чеки: БФД и запомненный контур. */
    var target by mutableStateOf(OfdTarget(environment = memory?.setupValue(ENVIRONMENT).orEmpty()))

    /** Пин будущего администратора кассы: набирает владелец, нигде не хранится. */
    var adminPin by mutableStateOf("")

    /** Отметка о том, что касса на другой машине замолчит. */
    var handoverAccepted by mutableStateOf(false)

    /** Незаполненный контур берёт прошлое значение, а в первый раз — первый поднятый. */
    fun preset(environments: List<OfdEnvironmentResponse>) {
        if (target.environment.isNotBlank()) return
        val raised = environments.firstOrNull { OfdContours.raised(it.code) }?.code ?: return
        target = target.copy(environment = raised)
    }

    /** Запоминает выбор владельца — но только после удавшегося заведения. */
    fun remember() {
        memory?.setupValue(ENVIRONMENT, target.environment)
    }

    /** Заполненное владельцем в том виде, в каком его читают правила. */
    fun form(handoverNeeded: Boolean) =
        AdoptForm(target.complete, adminPin, handoverNeeded, handoverAccepted)

    private companion object {
        const val ENVIRONMENT = "environment"
    }
}
