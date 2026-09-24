package kz.mybrain.superkassa.domain.setup.usecase

import kz.mybrain.superkassa.domain.setup.port.SetupCabinet

/**
 * Выпускает токен кассы в миг её заведения.
 *
 * Токен — ключ, которым касса подписывает запросы: он идёт из кабинета
 * в кассу внутри одного действия и нигде не задерживается — ни на экране,
 * ни в пройденном.
 */
class IssueKkmToken(private val cabinet: SetupCabinet) {

    /** @return токен; `null` — кабинет его не выдал, и кассу заводить нечем. */
    suspend operator fun invoke(registerId: String): String? = cabinet.issueToken(registerId)
}
