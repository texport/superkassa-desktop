package kz.mybrain.superkassa.integrations.bfdcabinet.applications

import io.ktor.http.HttpMethod
import kz.mybrain.superkassa.integrations.bfdcabinet.CABINET_PAGE_SIZE
import kz.mybrain.superkassa.integrations.bfdcabinet.CabinetPage
import kz.mybrain.superkassa.integrations.bfdcabinet.transport.CabinetLink
import kz.mybrain.superkassa.integrations.bfdcabinet.transport.inPath
import kz.mybrain.superkassa.integrations.bfdcabinet.transport.query

/**
 * Заявления в ИСНА.
 *
 * Все три идут одним путём: кабинет готовит то, что нужно подписать,
 * владелец подписывает ЭЦП, кабинет отправляет подписанное. Подпись между
 * шагами — дело приложения: шаги ждут каждый своего, и владелец должен
 * видеть, какого.
 */
class ApplicationsApi internal constructor(private val link: CabinetLink) {

    /** Готовит заявление: кабинет выдаёт, что подписать. */
    suspend fun prepare(application: CabinetApplication): ApplicationPrepared {
        val path = path(application, "application")
        return when (application) {
            is CabinetApplication.Registration -> link.post(path)
            is CabinetApplication.Reregistration -> link.send(HttpMethod.Post, path, application.request)
            is CabinetApplication.Deregistration -> link.send(HttpMethod.Post, path, application.request)
        }
    }

    /** Отправляет подписанное заявление в ИСНА. */
    suspend fun send(application: CabinetApplication, sign: SignRequest): ApplicationSent =
        link.send(HttpMethod.Post, path(application, "sign"), sign)

    /** Журнал регистрационных действий кассы — первая страница, свежие первыми. */
    suspend fun actions(registerId: String): List<RegistrationAction> =
        link.get<CabinetPage<RegistrationAction>>(
            "${register(registerId)}/registration-actions" + query("page" to 0, "size" to CABINET_PAGE_SIZE)
        ).items

    /** Одно регистрационное действие. */
    suspend fun action(registerId: String, actionId: String): RegistrationAction =
        link.get("${register(registerId)}/registration-actions/${actionId.inPath()}")

    private fun path(application: CabinetApplication, step: String): String {
        val kind = when (application) {
            is CabinetApplication.Registration -> "registration"
            is CabinetApplication.Reregistration -> "reregistration"
            is CabinetApplication.Deregistration -> "deregistration"
        }
        return "${register(application.registerId)}/$kind/$step"
    }

    private fun register(id: String) = "/api/cash-registers/${id.inPath()}"
}
