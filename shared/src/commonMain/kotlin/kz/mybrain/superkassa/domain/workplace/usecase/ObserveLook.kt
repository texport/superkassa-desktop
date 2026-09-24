package kz.mybrain.superkassa.domain.workplace.usecase

import kotlinx.coroutines.flow.StateFlow
import kz.mybrain.superkassa.domain.workplace.model.LookChoice
import kz.mybrain.superkassa.domain.workplace.model.WorkplaceLook

/**
 * Вид рабочего места — сейчас и при каждой смене.
 *
 * Без `suspend`: сценарий ничего не делает, а отдаёт поток, на который
 * модель вида подписывается сама.
 */
class ObserveLook(private val look: WorkplaceLook) {
    operator fun invoke(): StateFlow<LookChoice> = look.state
}
