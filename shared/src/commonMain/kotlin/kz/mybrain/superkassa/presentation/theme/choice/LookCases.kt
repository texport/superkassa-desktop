package kz.mybrain.superkassa.presentation.theme.choice

import kz.mybrain.superkassa.domain.workplace.model.WorkplaceLook
import kz.mybrain.superkassa.domain.workplace.usecase.ChooseLook
import kz.mybrain.superkassa.domain.workplace.usecase.ObserveLook

/** Сценарии вида окна: следить за выбором и менять его. */
class LookCases(look: WorkplaceLook) {
    val observe = ObserveLook(look)
    val choose = ChooseLook(look)
}
