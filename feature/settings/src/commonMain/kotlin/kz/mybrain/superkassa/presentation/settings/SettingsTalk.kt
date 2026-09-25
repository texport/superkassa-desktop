package kz.mybrain.superkassa.presentation.settings

import io.github.texport.superkassa.core.presentation.api.model.ofd.OfdCommandResponse
import kz.mybrain.superkassa.presentation.common.message.Message
import kz.mybrain.superkassa.presentation.common.model.Talk

/**
 * БФД команду не выполнил: его слова — кассиру, код — журналу.
 *
 * Слов у ответа может не быть — тогда кассир читает, что именно
 * не выполнено, как при любом сбое.
 */
internal fun Talk.refusedByOfd(what: String, action: String, answer: OfdCommandResponse) {
    val code = "OFD_${answer.resultCode ?: answer.status}"
    journal.warn("$action: refused $code")
    val words = answer.errorMessage ?: answer.resultText
    say(action, words?.let { Message.Refusal(it, code) } ?: Message.Failed(what))
}
