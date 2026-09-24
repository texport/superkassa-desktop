package kz.mybrain.superkassa.domain.users.usecase

import io.github.texport.superkassa.core.presentation.api.model.user.UserCreateRequest
import io.github.texport.superkassa.core.presentation.api.model.user.UserResponse
import io.github.texport.superkassa.core.presentation.api.model.user.UserRole
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.signin.model.askSeated
import kz.mybrain.superkassa.domain.signin.port.SignedKkm
import kz.mybrain.superkassa.domain.users.model.UserRules

/**
 * Заводит кассира с пином, который задал администратор.
 *
 * Пина по умолчанию нет: кассир получает только набранный. Имя уходит без
 * пробелов по краям — «Дана » и «Дана» в списке неразличимы.
 */
class CreateCashier(private val kassa: Kassa, private val signed: SignedKkm) {

    /** @return заведённый кассир; `null` — заводить нельзя: не тот пин или нет имени. */
    suspend operator fun invoke(name: String, role: UserRole, pin: String): Answer<UserResponse>? {
        if (!UserRules.canCreate(name, pin)) return null
        val request = UserCreateRequest(name.trim(), role, pin)
        return kassa.askSeated(signed) { api, seat -> api.createUser(seat.kkmId, seat.pin, request) }
    }
}
