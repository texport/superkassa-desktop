package kz.mybrain.superkassa.desktop.ui.sale

import kz.mybrain.superkassa.desktop.app.Message
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.ui.strings.SaleStrings
import kz.mybrain.superkassa.desktop.ui.strings.blockReasonWords

/**
 * Почему поиск по коду не дал позиции.
 *
 * Три разные беды, а не одна: товара нет в справочнике, справочник сейчас
 * не спросить, касса заблокирована. Кассир действует по каждой по-своему —
 * заводит позицию руками, зовёт обслуживание, разбирается с блокировкой, —
 * и одна фраза «нет такого штрихкода» на все три отправляла его искать
 * несуществующую беду с товаром.
 */
enum class LookupProblem {
    /** Справочник ответил, и такого кода у него нет. */
    Missing,

    /** Спросить не удалось: узел молчит, ответа нет или БФД отказал. */
    Unavailable,

    /** Касса заблокирована: справочник ей сейчас не отвечает. */
    Blocked
}

/**
 * Чем кончился поиск — по тому, что сказал узел.
 *
 * Молчание узла о беде и есть признак честного отсутствия: [Session.guard]
 * оставляет сообщение пустым только тогда, когда обращение прошло и узел
 * ответил по существу.
 */
fun lookupProblemOf(session: Session): LookupProblem {
    val message = session.lastMessage
    return when {
        (message as? Message.Refusal)?.code == KKM_BLOCKED -> LookupProblem.Blocked
        message != null -> LookupProblem.Unavailable
        else -> LookupProblem.Missing
    }
}

/**
 * Беда словами кассира и с тем, что делать сейчас.
 *
 * Причину блокировки называет общее место — то же, откуда её берут
 * главный экран и смена: своя фраза здесь развела бы одну блокировку
 * на два разных объяснения.
 */
fun lookupProblemWords(problem: LookupProblem, session: Session, texts: SaleStrings): String = when (problem) {
    LookupProblem.Missing -> texts.barcodeMissing
    LookupProblem.Unavailable -> texts.barcodeUnavailable
    LookupProblem.Blocked -> blockReasonWords(session.selected?.blockReasonCode, session.language)
}

private const val KKM_BLOCKED = "KKM_BLOCKED"
