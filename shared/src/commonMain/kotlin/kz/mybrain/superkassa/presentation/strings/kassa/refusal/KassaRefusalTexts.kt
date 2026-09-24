package kz.mybrain.superkassa.presentation.strings.kassa.refusal

import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.model.sale.UNKNOWN_VAT
import kz.mybrain.superkassa.presentation.common.format.fill
import kz.mybrain.superkassa.presentation.strings.common.Language

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
 *    годятся кассиру: в них код ставки, вида оплаты или единицы
 *    («VAT_12», «MOBILE», «999»), имя поля запроса («parentTicket»,
 *    «items[0].price») или нет того, что делать. Таких кодов — перечень
 *    [OWN_WORDS]; отказ, который приложение ставит само до кассы
 *    ([UNKNOWN_VAT]), тоже здесь: своих слов у него нет.
 * 3. Слов ядра на языке интерфейса нет — пусто или тот же текст, что
 *    по-английски, — и своей формулировки нет: общая фраза с кодом отказа.
 *    Код кассир назовёт поддержке.
 */
fun kassaRefusalWords(refused: Answer.Refused, language: Language): String {
    val texts = kassaRefusalTexts(language)
    OWN_WORDS[refused.code]?.let { return it(texts) }
    val core = when (language) {
        Language.Ru -> refused.ru
        Language.Kk -> refused.kk
        Language.En -> refused.en
    }
    val translated = core.isNotBlank() && (language == Language.En || core != refused.en)
    return if (translated) core else texts.unknown.fill(refused.code)
}

/**
 * Свои слова отказов кассы.
 *
 * @property unknown отказ без годных слов; `%s` — код отказа.
 */
data class KassaRefusalTexts(
    val programming: String,
    val vatNotPayer: String,
    val vatUnknown: String,
    val paymentUnsupported: String,
    val unitUnknown: String,
    val outOfRange: String,
    val basisRequired: String,
    val unknown: String
)

fun kassaRefusalTexts(language: Language): KassaRefusalTexts = when (language) {
    Language.Kk -> refusalKk
    Language.Ru -> refusalRu
    Language.En -> refusalEn
}

/** Коды, слова ядра на которых кассиру не годятся, — и свои слова для них. */
private val OWN_WORDS: Map<String, (KassaRefusalTexts) -> String> = mapOf(
    "KKM_IN_PROGRAMMING" to { it.programming },
    "RECEIPT_VAT_NOT_ALLOWED" to { it.vatNotPayer },
    UNKNOWN_VAT to { it.vatUnknown },
    "PAYMENT_TYPE_NOT_SUPPORTED" to { it.paymentUnsupported },
    "MEASURE_UNIT_CODE_INVALID" to { it.unitUnknown },
    "RECEIPT_VALUE_OUT_OF_RANGE" to { it.outOfRange },
    "PARENT_TICKET_REQUIRED" to { it.basisRequired }
)

private val refusalRu = KassaRefusalTexts(
    programming = "Касса в режиме программирования: выйдите из него в настройках кассы",
    vatNotPayer = "Касса не плательщик НДС: поставьте позициям «Без НДС»",
    vatUnknown = "Такую ставку НДС касса не знает: выберите ставку из списка",
    paymentUnsupported = "Этот вид оплаты касса сейчас не принимает: выберите другой",
    unitUnknown = "Такую единицу измерения касса не знает: выберите единицу из списка",
    outOfRange = "Цена, количество или скидка вне допустимого: проверьте позиции и оплату",
    basisRequired = "Для возврата выберите чек-основание",
    unknown = "Касса отказала, код отказа %s"
)

private val refusalKk = KassaRefusalTexts(
    programming = "Касса бағдарламалау режимінде: касса баптауларында одан шығыңыз",
    vatNotPayer = "Касса ҚҚС төлеушісі емес: позицияларға «ҚҚС-сыз» қойыңыз",
    vatUnknown = "Касса мұндай ҚҚС мөлшерлемесін білмейді: тізімнен мөлшерлеме таңдаңыз",
    paymentUnsupported = "Касса бұл төлем түрін қазір қабылдамайды: басқасын таңдаңыз",
    unitUnknown = "Касса мұндай өлшем бірлігін білмейді: тізімнен бірлік таңдаңыз",
    outOfRange = "Баға, саны немесе жеңілдік рұқсат етілгеннен тыс: позициялар мен төлемді тексеріңіз",
    basisRequired = "Қайтару үшін негіз чекті таңдаңыз",
    unknown = "Касса бас тартты, бас тарту коды %s"
)

private val refusalEn = KassaRefusalTexts(
    programming = "The register is in programming mode: leave it in the register settings",
    vatNotPayer = "The register is not a VAT payer: set the items to “No VAT”",
    vatUnknown = "The register does not know this VAT rate: choose a rate from the list",
    paymentUnsupported = "The register does not accept this payment type now: choose another one",
    unitUnknown = "The register does not know this unit: choose a unit from the list",
    outOfRange = "A price, quantity or discount is out of range: check the items and the payment",
    basisRequired = "Choose the original receipt for the refund",
    unknown = "The register refused, refusal code %s"
)
