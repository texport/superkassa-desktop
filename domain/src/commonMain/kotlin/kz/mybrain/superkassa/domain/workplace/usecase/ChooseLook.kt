package kz.mybrain.superkassa.domain.workplace.usecase

import kz.mybrain.superkassa.domain.workplace.model.LookChoice
import kz.mybrain.superkassa.domain.workplace.model.WorkplaceLook

/**
 * Кассир выбрал язык, оформление или свернул часть окна.
 *
 * Выбор действует сразу и переживает перезапуск: кассир меняет его
 * один раз под свой монитор и свою привычку, а не каждое утро.
 */
class ChooseLook(private val look: WorkplaceLook) {
    operator fun invoke(chosen: LookChoice) = look.choose(chosen)
}
