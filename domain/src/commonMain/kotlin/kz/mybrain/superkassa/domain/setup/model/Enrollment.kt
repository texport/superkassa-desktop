package kz.mybrain.superkassa.domain.setup.model

import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import kz.mybrain.superkassa.domain.kassa.model.Answer

/**
 * Что заводится: контур, идентификатор кассы у БФД, пин администратора
 * и название из кабинета.
 *
 * Пин администратора — единственный пин новой кассы: стандартного у неё
 * нет, и войти в неё можно только им. В строку плана он не попадает:
 * строка уходит в журналы.
 *
 * @property systemId идентификатор у БФД; `null` — кассу ещё не завели
 *   в кабинете, и заводить здесь нечего.
 */
data class EnrollmentPlan(
    val contour: String,
    val systemId: String?,
    val adminPin: String,
    val name: String? = null
) {
    override fun toString(): String = "EnrollmentPlan(contour=$contour, systemId=$systemId)"
}

/** Чем кончилось заведение кассы. */
sealed interface EnrollOutcome {

    /** Касса заведена и читается. */
    data class Enrolled(val kkm: KkmResponse) : EnrollOutcome

    /**
     * Касса ответила на заведение успехом, а читать нечего.
     *
     * Так отвечает касса процесса, когда БФД не подтвердила кассу: сведения
     * о кассе есть, а в базе её нет. Подключённой такая касса не названа.
     */
    data object NotStored : EnrollOutcome

    /** Касса отказала или не смогла: её код и слова. */
    data class Refused(val answer: Answer<Nothing>) : EnrollOutcome

    /** Заводить нечем: нет идентификатора или кабинет не выдал токен. */
    data object Skipped : EnrollOutcome
}
