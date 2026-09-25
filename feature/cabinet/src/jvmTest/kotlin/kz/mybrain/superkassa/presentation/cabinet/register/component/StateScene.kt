package kz.mybrain.superkassa.presentation.cabinet.register.component

import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.domain.cabinet.model.documents.TechnicalState
import kz.mybrain.superkassa.kassa.CoreScene

/** Показания трёх источников о кассе 5000021 — для проверок сверки. */
internal object StateScene {

    /** Касса этой машины: её состояние и открыта ли у неё смена. */
    fun node(state: String, shiftOpen: Boolean = false) =
        CoreScene.kkm(state = state, id = "k-1").copy(ofdSystemId = "5000021", isShiftOpen = shiftOpen)

    fun cabinetRegister(status: String) =
        CabinetRegister(id = "r-1", kkmId = 5_000_021, status = status)

    fun bfd(active: Boolean?, shift: String?) =
        TechnicalState(found = true, active = active, shiftStatus = shift)

    fun answer(claims: List<StateClaim>, question: StateQuestion): StateAnswer =
        stateAnswers(claims).first { it.question == question }
}
