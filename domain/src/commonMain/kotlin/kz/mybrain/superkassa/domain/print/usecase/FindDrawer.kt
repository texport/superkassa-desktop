package kz.mybrain.superkassa.domain.print.usecase

import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmListParams
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.model.ask
import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.signin.model.SignIn

/**
 * Касса, которая рисует печатную форму.
 *
 * Форму рисует касса, а не приложение. Кассир при этом мог и не входить:
 * владелец открывает кабинет с экрана входа и смотрит там чужие чеки.
 * Рисует выбранная касса, иначе первая заведённая на этом месте.
 */
class FindDrawer(private val kassa: Kassa, private val signIn: SignIn) {

    /** @return касса-рисовальщик; `null` внутри — на месте не заведено ни одной. */
    suspend operator fun invoke(): Answer<KkmResponse?> {
        signIn.state.value.kkm?.let { return Answer.Done(it) }
        return kassa.ask { it.listKkms(KkmListParams(limit = 1)).items.firstOrNull() }
    }
}
