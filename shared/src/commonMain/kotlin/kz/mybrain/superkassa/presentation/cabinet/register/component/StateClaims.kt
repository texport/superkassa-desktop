package kz.mybrain.superkassa.presentation.cabinet.register.component

import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.domain.cabinet.model.documents.TechnicalState
import kz.mybrain.superkassa.domain.cabinet.model.kkmRecord
import kz.mybrain.superkassa.domain.cabinet.model.onRecord

/** Касса этой машины: заблокированная касса фискальных команд не выполняет. */
internal fun nodeClaim(kkm: KkmResponse?): StateClaim = StateClaim(
    source = StateSource.Node,
    usable = when (kkm?.state) {
        null -> Verdict.Unknown
        ACTIVE -> Verdict.Yes
        else -> Verdict.No
    },
    shift = when {
        kkm == null -> Verdict.Unknown
        kkm.isShiftOpen -> Verdict.Yes
        else -> Verdict.No
    }
)

/**
 * Кабинет: на учёте стоит только зарегистрированная касса, а смену
 * он не ведёт вовсе — у него учёт КГД, а не работа машины.
 *
 * Пустой статус — незнание, а не отказ. Прежде на его месте стояла
 * ветка `null`, которой быть не может: поле обязательное, — и пустая
 * строка уходила под `else`, то есть читалась как «снята с учёта».
 */
internal fun cabinetClaim(register: CabinetRegister): StateClaim = StateClaim(
    source = StateSource.Cabinet,
    usable = when {
        register.status.isBlank() -> Verdict.Unknown
        onRecord(register.status) -> Verdict.Yes
        else -> Verdict.No
    },
    shift = Verdict.Unknown,
    record = register.status.takeIf { it.isNotBlank() }?.let(::kkmRecord)
)

/** БФД: свой снимок кассы, которой он может и не знать вовсе. */
internal fun bfdClaim(technical: TechnicalState?): StateClaim = StateClaim(
    source = StateSource.Bfd,
    usable = when {
        technical?.found != true || technical.active == null -> Verdict.Unknown
        technical.active == true -> Verdict.Yes
        else -> Verdict.No
    },
    shift = when (technical?.shiftStatus) {
        null -> Verdict.Unknown
        SHIFT_OPEN -> Verdict.Yes
        else -> Verdict.No
    }
)

/** Состояние кассы, при котором она работает. */
private const val ACTIVE = "ACTIVE"

/** Состояние смены в снимке БФД, означающее открытую смену. */
private const val SHIFT_OPEN = "OPEN"
