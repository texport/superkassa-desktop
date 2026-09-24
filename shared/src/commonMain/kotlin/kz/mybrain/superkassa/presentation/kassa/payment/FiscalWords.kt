package kz.mybrain.superkassa.presentation.kassa.payment

import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.model.Fiscal
import kz.mybrain.superkassa.domain.kassa.model.FiscalOutcome
import kz.mybrain.superkassa.domain.kassa.model.outcome
import kz.mybrain.superkassa.presentation.common.message.Message
import kz.mybrain.superkassa.presentation.common.message.deliveryReport
import kz.mybrain.superkassa.presentation.common.model.Talk
import kz.mybrain.superkassa.presentation.strings.common.AppStrings
import kz.mybrain.superkassa.presentation.strings.common.of
import kz.mybrain.superkassa.presentation.strings.journal.ofdRefusalWords
import kz.mybrain.superkassa.presentation.strings.kassa.paymentTexts

/**
 * Как фискальная операция названа кассиру и журналу.
 *
 * @property what что делается, словами кассира: «Продажа».
 * @property done что сделано, словами кассира: «Внесено 1 000 ₸».
 * @property action что делается, для журнала: `issue receipt`.
 */
class FiscalWords(val what: String, val done: String, val action: String)

/**
 * Итог фискальной операции словами кассира.
 *
 * Одна дорога для чека, возврата и денег: итог объявляется одинаково.
 * Прежде у трёх экранов было три способа сказать об одном и том же.
 *
 * Принят — сказано, дошёл ли документ до БФД или ждёт в автономной
 * очереди. Отвергнут БФД — это отказ, а не итог: прежде он приходил
 * зелёной строкой «Продажа. состояние доставки: Отклонён», корзина
 * очищалась, и кассир отпускал покупателя с чеком, которого в БФД нет.
 * Отказ кассы — её словами на языке кассира, в том числе запертый пин
 * с оставшимся временем. Нет ответа — документ мог пройти: кассиру
 * сказано проверить журнал, а повтор с тем же ключом второго документа
 * не создаст.
 */
fun Talk.fiscal(answer: Answer<Fiscal>, words: FiscalWords, texts: AppStrings): FiscalOutcome {
    when (answer) {
        is Answer.Done -> if (answer.value.rejected) {
            rejected(answer.value, words, texts)
        } else {
            done(deliveryReport(words.done, answer.value.delivery, texts), words.action)
        }
        is Answer.Refused -> shown(answer, words.what, words.action)
        is Answer.Failed -> {
            journal.failure("${words.action}: no answer ${answer.reason}")
            notices.show(Message.NoAnswer(texts.common.noAnswer))
        }
    }
    return answer.outcome
}

/** БФД отверг документ: причина его кодом, и что делать с набранным. */
private fun Talk.rejected(fiscal: Fiscal, words: FiscalWords, texts: AppStrings) {
    val code = fiscal.refusalCode
    journal.warn("${words.action}: rejected by bfd ${code ?: "without code"}")
    // Слова кассы о причине — на языке кассира; без них — свой справочник кодов.
    val reason = fiscal.refusal?.of(language()) ?: ofdRefusalWords(code, language())
    val text = listOfNotNull("${words.what}: ${texts.status.refused}", reason, paymentTexts(language()).rejectedKept)
        .joinToString(". ")
    say(words.action, Message.Refusal(text, code?.toString() ?: REJECTED))
}

/** Код отказа, когда касса кода БФД не назвала. */
private const val REJECTED = "ONLINE_ERROR"
