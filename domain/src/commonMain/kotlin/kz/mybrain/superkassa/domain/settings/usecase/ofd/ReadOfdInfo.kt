package kz.mybrain.superkassa.domain.settings.usecase.ofd

import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.model.map
import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.settings.model.OfdSummary
import kz.mybrain.superkassa.domain.signin.model.askSeated
import kz.mybrain.superkassa.domain.signin.port.SignedKkm
import kz.mybrain.superkassa.domain.signin.usecase.RefreshKkm

/**
 * Сведения БФД о кассе, сведённые к тому, что читает кассир.
 *
 * Запрос сведений — не только чтение: ответ БФД «всё в порядке» снимает
 * с кассы блокировку, наложенную его прежним отказом. Касса перечитывается
 * после запроса, иначе шапка, продажа и настройки показывали бы кассу
 * вставшей, хотя она уже снова в строю.
 */
class ReadOfdInfo(private val kassa: Kassa, private val signed: SignedKkm) {

    /** @param language код языка кассира: ответ и адрес ОФД даёт на нескольких. */
    suspend operator fun invoke(language: String): Answer<OfdSummary> {
        val answer = kassa.askSeated(signed) { api, seat -> api.getOfdInfo(seat.kkmId) }
        RefreshKkm(kassa, signed)()
        return answer.map { OfdSummary.of(it.responseJson, language) }
    }
}
