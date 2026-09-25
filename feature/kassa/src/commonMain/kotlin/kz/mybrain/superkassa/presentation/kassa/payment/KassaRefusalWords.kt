package kz.mybrain.superkassa.presentation.kassa.payment

import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.fill
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Отказ кассы в продаже, возврате и движении денег — словами кассира.
 *
 * Правило выбора слов:
 *
 * 1. Слова ядра идут кассиру как есть, когда они понятны кассиру и есть
 *    на языке интерфейса. Ядро говорит на трёх языках, и пересказ своими
 *    словами разошёлся бы с ним: «В кассе недостаточно наличных»,
 *    «Смена не открыта», «По этому чеку уже возвращено всё» — остаются.
 * 2. Своя формулировка по коду отказа — только там, где слова ядра не
 *    годятся кассиру (перечень — у текстов отказа, `KassaRefusalTexts.own`);
 *    отказ, который приложение ставит само до кассы (неизвестная ставка НДС),
 *    тоже там: своих слов у него нет.
 * 3. Слов ядра на языке интерфейса нет — пусто или тот же текст, что
 *    по-английски, — и своей формулировки нет: общая фраза с кодом отказа.
 *    Код кассир назовёт поддержке.
 */
internal fun kassaRefusalWords(refused: Answer.Refused, language: Language): String {
    val texts = textsOf(language).kassa.refusal
    texts.own(refused.code)?.let { return it }
    val core = when (language) {
        Language.Ru -> refused.ru
        Language.Kk -> refused.kk
        Language.En -> refused.en
    }
    val translated = core.isNotBlank() && (language == Language.En || core != refused.en)
    return if (translated) core else texts.unknown.fill(refused.code)
}
