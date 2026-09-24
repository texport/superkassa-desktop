package kz.mybrain.superkassa.domain.kassa.model.entry

import io.github.texport.superkassa.core.presentation.api.model.ofd.NomenclatureLookupResponse
import kz.mybrain.superkassa.domain.kassa.model.Answer

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

    /** Спросить не удалось: БФД не ответил, касса отказала или не смогла. */
    Unavailable,

    /** Касса заблокирована: справочник ей сейчас не отвечает. */
    Blocked
}

/**
 * Чем кончился поиск — по ответу кассы; `null` — товар найден.
 *
 * Касса называет неотвеченный запрос отрицательным кодом результата:
 * БФД своих кодов меньше нуля не даёт, и такой ответ — не «нет товара»,
 * а «справочник не спросили».
 */
fun lookupProblemOf(answer: Answer<NomenclatureLookupResponse>): LookupProblem? = when (answer) {
    is Answer.Done -> when {
        answer.value.found && answer.value.item != null -> null
        answer.value.resultCode < 0 -> LookupProblem.Unavailable
        else -> LookupProblem.Missing
    }
    is Answer.Refused -> if (answer.code == KKM_BLOCKED) LookupProblem.Blocked else LookupProblem.Unavailable
    is Answer.Failed -> LookupProblem.Unavailable
}

private const val KKM_BLOCKED = "KKM_BLOCKED"
