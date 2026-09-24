package kz.mybrain.superkassa.domain.cabinet

import kotlinx.coroutines.flow.MutableStateFlow
import kz.mybrain.superkassa.domain.cabinet.model.CabinetCompany
import kz.mybrain.superkassa.domain.cabinet.model.CabinetOwner
import kz.mybrain.superkassa.domain.cabinet.model.CabinetUser
import kz.mybrain.superkassa.domain.cabinet.port.CabinetAccount
import kz.mybrain.superkassa.domain.cabinet.port.CabinetAddresses
import kz.mybrain.superkassa.domain.cabinet.port.CabinetApplications
import kz.mybrain.superkassa.domain.cabinet.port.CabinetCards
import kz.mybrain.superkassa.domain.cabinet.port.CabinetCompanies
import kz.mybrain.superkassa.domain.cabinet.port.CabinetDocuments
import kz.mybrain.superkassa.domain.cabinet.port.CabinetPlaces
import kz.mybrain.superkassa.domain.cabinet.port.CabinetPorts
import kz.mybrain.superkassa.domain.cabinet.port.CabinetRegisters
import kz.mybrain.superkassa.domain.cabinet.port.SavedFiles
import kz.mybrain.superkassa.domain.cabinet.port.Signer
import java.lang.reflect.Proxy

/**
 * Порт, у которого проверка задала только нужное ей.
 *
 * Остальные ручки падают, называя себя: проверка, задевшая лишнее,
 * видна сразу, а не отвечает пустотой за кабинет.
 */
internal inline fun <reified T : Any> unwired(): T =
    Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { proxy, method, args ->
        when (method.name) {
            "toString" -> "unwired ${T::class.simpleName}"
            "hashCode" -> System.identityHashCode(proxy)
            "equals" -> proxy === args?.firstOrNull()
            else -> error("${T::class.simpleName}.${method.name} is not wired in this check")
        }
    } as T

/** Вход в кабинет без кабинета: владелец задан проверкой. */
internal class TestAccount(owner: CabinetOwner? = null) : CabinetAccount {
    override val owner = MutableStateFlow(owner)
    override val address = "http://cabinet.test"

    override suspend fun signIn() {
        owner.value = OWNER
    }

    override suspend fun signOut() {
        owner.value = null
    }

    companion object {
        val OWNER = CabinetOwner(
            CabinetUser(id = "u-1", iin = "870101300123", fullName = "Иванов Сергей"),
            CabinetCompany(id = "c-1", bin = "180140000123", name = "ТОО «Пример»")
        )
    }
}

/** Порты кабинета для проверок моделей и сценариев: каждый задаётся по месту. */
internal class TestPorts : CabinetPorts {
    override var account: CabinetAccount = TestAccount()
    override var company: CabinetCompanies = unwired()
    override var places: CabinetPlaces = unwired()
    override var addresses: CabinetAddresses = unwired()
    override var registers: CabinetRegisters = unwired()
    override var applications: CabinetApplications = unwired()
    override var cards: CabinetCards = unwired()
    override var documents: CabinetDocuments = unwired()
    override var signer: Signer = unwired()
    override var files: SavedFiles = unwired()
}
