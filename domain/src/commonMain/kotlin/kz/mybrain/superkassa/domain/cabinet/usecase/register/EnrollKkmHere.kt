package kz.mybrain.superkassa.domain.cabinet.usecase.register

import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmInitDirectRequest
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.model.ask
import kz.mybrain.superkassa.domain.kassa.port.Kassa

/**
 * Заводит кассу кабинета на этой машине и убеждается, что она заведена.
 *
 * Пин администратора задаёт владелец и передаёт его в запросе заведения:
 * пинов по умолчанию у кассы нет, своего пина приложение не подставляет. Касса при недоступном
 * ОФД отвечает на заведение успехом и кассой, которой в её базе нет, —
 * заведённое проверяется повторным чтением. Название, данное владельцем
 * в кабинете, уходит в кассу сразу; отказ здесь заведение не рвёт.
 */
class EnrollKkmHere(private val kassa: Kassa) {
    suspend operator fun invoke(
        register: CabinetRegister,
        provider: String,
        environment: String,
        adminPin: String,
        token: String
    ): Answer<KkmResponse> {
        val request = KkmInitDirectRequest(
            ofdId = provider,
            ofdEnvironment = environment,
            ofdSystemId = register.kkmId.toString(),
            ofdToken = token,
            kkmKgdId = register.registrationNumber.orEmpty(),
            factoryNumber = register.factoryNumber.orEmpty(),
            manufactureYear = register.manufactureYear,
            adminPin = adminPin
        )
        val created = kassa.ask { it.initKkm(request) }
        if (created !is Answer.Done) return created
        val kkm = kassa.ask { it.getKkm(created.value.kkmId) }
        register.internalName?.takeIf { it.isNotBlank() && kkm is Answer.Done }?.let { name ->
            kassa.ask { it.updateKkmName(created.value.kkmId, adminPin, name) }
        }
        return kkm
    }
}
