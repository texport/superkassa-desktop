package kz.mybrain.superkassa.data.cabinet.register

import kz.mybrain.superkassa.data.cabinet.cabinetCall
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.domain.cabinet.model.KkmModel
import kz.mybrain.superkassa.domain.cabinet.model.RegisterCreate
import kz.mybrain.superkassa.domain.cabinet.model.RegisterEdit
import kz.mybrain.superkassa.domain.cabinet.model.TokenIssued
import kz.mybrain.superkassa.domain.cabinet.model.documents.RegisterState
import kz.mybrain.superkassa.domain.cabinet.port.CabinetRegisters
import kz.mybrain.superkassa.integrations.bfdcabinet.analytics.AnalyticsApi
import kz.mybrain.superkassa.integrations.bfdcabinet.analytics.PositionSource
import kz.mybrain.superkassa.integrations.bfdcabinet.register.RegistersApi
import kz.mybrain.superkassa.integrations.bfdcabinet.register.RegisterCreate as BfdCreate
import kz.mybrain.superkassa.integrations.bfdcabinet.register.RegisterEdit as BfdEdit

/**
 * Кассы — модулем кабинета от имени вошедшего.
 *
 * Блокировку кабинет отдаёт не в списке касс, а сводкой карты касс
 * аналитики — одним обращением на все кассы компании.
 */
internal class RemoteRegisters(
    private val registers: RegistersApi,
    private val analytics: AnalyticsApi
) : CabinetRegisters {
    override suspend fun all(onPart: (List<CabinetRegister>, Long) -> Unit): List<CabinetRegister> {
        val all = cabinetCall { registers.all { part, total -> onPart(part.map { it.register() }, total) } }
        return all.map { it.register() }
    }

    override suspend fun one(id: String): CabinetRegister = cabinetCall { registers.one(id) }.register()

    override suspend fun add(register: RegisterCreate): CabinetRegister {
        val create = with(register) {
            BfdCreate(retailPlaceId, modelCode, factoryNumber, manufactureYear, internalName)
        }
        return cabinetCall { registers.add(create) }.register()
    }

    override suspend fun edit(id: String, edit: RegisterEdit): CabinetRegister {
        val change = with(edit) { BfdEdit(retailPlaceId, modelCode, factoryNumber, manufactureYear) }
        return cabinetCall { registers.edit(id, change) }.register()
    }

    override suspend fun rename(id: String, name: String): CabinetRegister =
        cabinetCall { registers.rename(id, name) }.register()

    override suspend fun remove(id: String) = cabinetCall { registers.remove(id) }

    override suspend fun state(id: String): RegisterState = cabinetCall { registers.state(id) }.state()

    override suspend fun blocked(): Set<String> {
        val view = cabinetCall { analytics.map(PositionSource.RetailPlaceAddress) }
        return (view.placed + view.withoutPosition).filter { it.blocked }.map { it.cashRegisterId }.toSet()
    }

    override suspend fun issueToken(id: String): TokenIssued =
        cabinetCall { registers.issueToken(id) }.let { TokenIssued(it.kkmId, it.token) }

    override suspend fun models(): List<KkmModel> =
        cabinetCall { registers.models() }.map { KkmModel(it.modelCode, it.name, it.active) }
}
