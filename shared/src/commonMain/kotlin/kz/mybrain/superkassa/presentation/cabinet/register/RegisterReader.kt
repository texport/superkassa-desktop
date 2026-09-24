package kz.mybrain.superkassa.presentation.cabinet.register

import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.domain.cabinet.model.documents.RegisterState
import kz.mybrain.superkassa.domain.cabinet.model.documents.RegistrationAction
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.model.ask
import kz.mybrain.superkassa.presentation.cabinet.CabinetViewModel
import kz.mybrain.superkassa.presentation.cabinet.value

/** Прочитанное о кассе; `null` в поле — это обращение не удалось. */
data class RegisterRead(
    val card: CabinetRegister?,
    val state: RegisterState?,
    val actions: List<RegistrationAction>?,
    val kkms: List<KkmResponse>?
)

/**
 * Чтение карточки кассы: три обращения, где беда одного не отменяет
 * остальных.
 *
 * Список отдаёт краткую запись кассы, а признак регистрационной карты
 * и последнее действие есть только в её карточке: без этого запроса блок
 * карты не открылся бы ни у одной кассы.
 */
internal class RegisterReader(private val cabinet: CabinetViewModel) {
    private val cases = cabinet.useCases

    /**
     * @param loud читает ли владелец: его чтение занимает окно и называет
     *   помеху, опрос — молчит.
     */
    suspend fun read(id: String, loud: Boolean): RegisterRead = RegisterRead(
        card = ask(loud, "read register") { cases.readCard(id) },
        state = ask(loud, "read register state") { cases.readState(id) },
        actions = ask(loud, "read registration actions") { cases.readActions(id) },
        // Кассы этой машины — молча: отказ кассы здесь не о том, что спрашивал владелец.
        kkms = (cases.readKkmsHere() as? Answer.Done)?.value
    )

    private suspend fun <T> ask(loud: Boolean, action: String, block: suspend () -> T): T? =
        if (loud) cabinet.work.run(action, block).value else cabinet.work.quiet(action, block)
}
