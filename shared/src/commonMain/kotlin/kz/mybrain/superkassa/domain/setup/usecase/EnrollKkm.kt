package kz.mybrain.superkassa.domain.setup.usecase

import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmInitSimpleRequest
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.model.ask
import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.log.port.Journal
import kz.mybrain.superkassa.domain.setup.model.EnrollOutcome
import kz.mybrain.superkassa.domain.setup.model.EnrollmentPlan
import kz.mybrain.superkassa.domain.setup.model.OfdContours

/**
 * Заводит кассу в кассе процесса и проверяет, что она читается.
 *
 * Ответ на заведение ещё ничего не доказывает: при молчащей БФД касса
 * процесса отвечает успехом и сведениями о кассе, которой в её базе нет.
 * Поэтому касса считается заведённой, только когда она читается.
 *
 * Пин администратора новой кассы — только тот, что набрал владелец:
 * пинов по умолчанию у кассы нет, и без пина она не заводится.
 *
 * Название из кабинета уходит сразу за заведением, пином новой кассы;
 * отказ в нём заведения не рвёт — безымянную кассу назовут из настроек.
 *
 * Единственное место приложения, где кассу заводят у кассы процесса.
 */
class EnrollKkm(private val kassa: Kassa, private val log: Journal) {

    /** @param token выдаёт токен кассы в миг заведения; `null` — токена нет. */
    suspend operator fun invoke(plan: EnrollmentPlan, token: suspend () -> String?): EnrollOutcome {
        val systemId = plan.systemId
        val issued = systemId?.let { token() }
        if (systemId == null || issued == null) return EnrollOutcome.Skipped
        val request = KkmInitSimpleRequest(
            ofdId = OfdContours.PROVIDER,
            ofdEnvironment = plan.contour,
            ofdSystemId = systemId,
            ofdToken = issued,
            adminPin = plan.adminPin
        )
        return when (val created = kassa.ask { it.initKkmSimple(request) }) {
            is Answer.Done -> verified(created.value, plan)
            is Answer.Refused -> EnrollOutcome.Refused(created)
            is Answer.Failed -> EnrollOutcome.Refused(created)
        }
    }

    private suspend fun verified(created: KkmResponse, plan: EnrollmentPlan): EnrollOutcome {
        val stored = (kassa.ask { it.getKkm(created.kkmId) } as? Answer.Done)?.value
        if (stored == null) log.warn("init kkm: answered, but kkm ${created.kkmId} is not stored")
        return stored?.let { EnrollOutcome.Enrolled(named(it, plan)) } ?: EnrollOutcome.NotStored
    }

    private suspend fun named(kkm: KkmResponse, plan: EnrollmentPlan): KkmResponse {
        log.info("kkm ${kkm.kkmId} created")
        val name = plan.name?.takeIf { it.isNotBlank() } ?: return kkm
        val answer = kassa.ask { it.updateKkmName(kkm.kkmId, plan.adminPin, name) }
        if (answer !is Answer.Done) log.warn("name new kkm: not named")
        return (answer as? Answer.Done)?.value ?: kkm
    }
}
