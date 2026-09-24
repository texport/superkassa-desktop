package kz.mybrain.superkassa.domain.settings.usecase

import io.github.texport.superkassa.core.presentation.api.model.ofd.OfdAuthInfoRequest
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.signin.model.askSeated
import kz.mybrain.superkassa.domain.signin.port.SignedKkm

/**
 * Номер следующего запроса кассы к БФД — и только он.
 *
 * БФД считает запросы своим счётом, и разошедшийся номер объясняет отказы,
 * которых иначе не понять. Номер касса отдаёт только администратору.
 *
 * Тем же ответом касса отдаёт и свой токен. Дальше этого сценария он
 * не уходит: по нему отправляют фискальные команды от имени кассы,
 * а экран настроек показывают и снимают — строка с токеном уехала бы
 * в чужой снимок вместе со всем, что рядом.
 */
class ReadNextRequest(private val kassa: Kassa, private val signed: SignedKkm) {

    suspend operator fun invoke(): Answer<Int> =
        kassa.askSeated(signed) { api, seat -> api.getOfdAuthInfo(seat.pin, OfdAuthInfoRequest(seat.kkmId)).nextReqNum }
}
