package kz.mybrain.superkassa.domain.signin.model

import io.github.texport.superkassa.core.presentation.api.SuperkassaApi
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.model.answering
import kz.mybrain.superkassa.domain.kassa.model.ask
import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.signin.port.SignedKkm

/**
 * Команда кассе, за которой работают, — с её номером и пином вошедшего.
 *
 * Общий ход сценариев всех областей: касса и пин берутся в момент вызова,
 * итог — ответ кассы, отказ или сбой. За кассой никто не работает — сбой
 * обращения: экраны с командами без входа не открываются, и дойти сюда
 * можно только ошибкой.
 */
suspend fun <T> Kassa.askSeated(signed: SignedKkm, request: (SuperkassaApi, Seat) -> T): Answer<T> {
    val seat = signed.seat() ?: return Answer.Failed(NO_SEAT)
    return ask { request(it, seat) }
}

/**
 * То же, что [askSeated], для обращения не к фасаду кассы, а к другому
 * порту ядра — доставке чека, печати: касса и пин берутся в момент вызова.
 */
suspend fun <T> SignedKkm.answerSeated(request: suspend (Seat) -> T): Answer<T> {
    val seat = seat() ?: return Answer.Failed(NO_SEAT)
    return answering { request(seat) }
}

/**
 * Команда, меняющая кассу: её итог сразу видят все разделы.
 *
 * Настройка меняет кассу, а её показывают шапка, продажа и вход. Касса
 * отвечает на команду собой целиком — со сменой и очередью досылки, — и
 * этот ответ сразу отдаётся разделам: перечитывать кассу вслед за командой
 * незачем.
 */
suspend fun Kassa.changeSeated(signed: SignedKkm, request: (SuperkassaApi, Seat) -> KkmResponse): Answer<KkmResponse> =
    askSeated(signed, request).also { if (it is Answer.Done) signed.refresh(it.value) }

/** Имя сбоя «за кассой никто не работает»: для журнала, не для кассира. */
private const val NO_SEAT = "NoKkmSelected"
