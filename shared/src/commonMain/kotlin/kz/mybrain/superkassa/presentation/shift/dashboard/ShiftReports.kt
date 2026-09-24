package kz.mybrain.superkassa.presentation.shift.dashboard

import kz.mybrain.superkassa.domain.shift.model.ShiftPart
import kz.mybrain.superkassa.domain.shift.model.ShiftSnapshot
import kz.mybrain.superkassa.domain.shift.model.ShiftTrouble
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.common.AppStrings
import kz.mybrain.superkassa.strings.api.textsOf

/** Принимает перечитанное: касса, смена, документы и наличные. */
internal fun DashboardUiState.adopt(snapshot: ShiftSnapshot): DashboardUiState = copy(
    kkm = snapshot.kkm ?: kkm,
    shift = snapshot.shift,
    shiftNumber = snapshot.open?.shiftNo,
    dayLimitAt = snapshot.open?.dayLimitAt,
    dayLimitExceeded = snapshot.open?.dayLimitExceeded == true,
    documents = snapshot.documents.orEmpty(),
    documentsRead = snapshot.documents != null,
    cashInDrawer = snapshot.cash,
    reading = false
)

/** Что не прочиталось — словами кассира. */
internal fun ShiftTrouble.words(texts: AppStrings, language: Language): String = when (part) {
    ShiftPart.Kkm -> texts.login.reload
    ShiftPart.Shift -> texts.dashboard.shift
    ShiftPart.Documents -> texts.dashboard.shiftDocuments
    ShiftPart.Cash -> textsOf(language).kassa.money.drawer.inDrawer
}

/** Что не прочиталось — для журнала. */
internal val ShiftTrouble.action: String
    get() = when (part) {
        ShiftPart.Kkm -> "read kkm"
        ShiftPart.Shift -> "read shift"
        ShiftPart.Documents -> "read shift documents"
        ShiftPart.Cash -> "read counters"
    }
