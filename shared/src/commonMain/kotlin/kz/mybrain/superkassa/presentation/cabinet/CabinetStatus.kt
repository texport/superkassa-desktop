package kz.mybrain.superkassa.presentation.cabinet

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import kz.mybrain.superkassa.presentation.common.status.Chip
import kz.mybrain.superkassa.presentation.common.status.StatusTone
import kz.mybrain.superkassa.presentation.common.status.toneColor
import kz.mybrain.superkassa.presentation.strings.cabinet.CabinetTexts

/**
 * Состояния кабинета словами и цветом.
 *
 * Кабинет отдаёт протокольные коды — `DRAFT`, `REGISTERED`,
 * `REREGISTRATION_IN_ISNA_PROCESS`. Владельцу они не говорят ничего, а по
 * правилам приложения кодов на экране быть не должно: их место в журнале
 * поддержки, а не в карточке кассы.
 *
 * Цвет несёт тот же смысл, что и везде в приложении: зелёный — сделано,
 * жёлтый — ждём ответа, красный — отказ, серый — законченное дело,
 * в котором ничего не случилось.
 */
@Composable
fun CabinetStatusChip(status: String?, texts: CabinetTexts) {
    val code = status?.takeIf { it.isNotBlank() } ?: return
    Chip(text = statusTitle(code, texts), color = statusColor(code))
}

/** Цвет состояния — по его роли, одной на всё приложение. */
@Composable
fun statusColor(code: String): Color = toneColor(statusTone(code))

/**
 * Роль состояния: сделано, законченное дело, отказ или ожидание.
 *
 * Отдельно от цвета, чтобы её можно было проверить: цвет берётся
 * из схемы и живёт только внутри разметки.
 */
fun statusTone(code: String): StatusTone = when (code.uppercase()) {
    in DONE -> StatusTone.Good
    in REFUSED -> StatusTone.Bad
    in SETTLED -> StatusTone.Idle
    else -> StatusTone.Waiting
}

/** Состояния, означающие сделанное. */
private val DONE = setOf("REGISTERED", "REGISTERED_REREGISTRATION_SUCCESS", "KKM_ACTIVE", "ACCEPTED", "OPEN")

/** Состояния, означающие отказ. */
private val REFUSED = setOf(
    "REJECTED",
    "REGISTRATION_IN_ISNA_ERROR",
    "REGISTERED_REREGISTRATION_ERROR",
    "DEREGISTRATION_ERROR"
)

/**
 * Состояния покоя: не удача, не беда и не ожидание.
 *
 * Касса, снятая с учёта по заявлению самого владельца, стояла в списке
 * красной плашкой рядом с работающими — и выглядела сломанной. Снятие
 * с учёта владелец затеял сам и довёл до конца; чинить здесь нечего.
 *
 * Черновик — то же самое с другого конца жизни кассы: он никуда
 * не подан, и ждать по нему нечего и некого. Жёлтым он читался как
 * заявление в работе, а у сети показа черновиков три тысячи из трёх
 * тысяч трёхсот — колонка стояла жёлтой сверху донизу рядом с тремя
 * кассами, по которым КГД и правда думает.
 */
private val SETTLED = setOf("DEREGISTERED", "DRAFT")
