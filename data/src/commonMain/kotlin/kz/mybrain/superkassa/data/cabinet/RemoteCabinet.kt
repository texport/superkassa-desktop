package kz.mybrain.superkassa.data.cabinet

import kz.mybrain.superkassa.data.cabinet.address.RemoteAddresses
import kz.mybrain.superkassa.data.cabinet.applications.RemoteApplications
import kz.mybrain.superkassa.data.cabinet.company.RemoteCompanies
import kz.mybrain.superkassa.data.cabinet.documents.RemoteDocuments
import kz.mybrain.superkassa.data.cabinet.places.RemotePlaces
import kz.mybrain.superkassa.data.cabinet.register.RemoteCards
import kz.mybrain.superkassa.data.cabinet.register.RemoteRegisters
import kz.mybrain.superkassa.data.cabinet.signin.RemoteAccount
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
import kz.mybrain.superkassa.domain.cabinet.port.Signing
import kz.mybrain.superkassa.domain.log.port.Journal
import kz.mybrain.superkassa.integrations.bfdcabinet.BfdCabinet
import kz.mybrain.superkassa.integrations.bfdcabinet.CabinetSettings
import kz.mybrain.superkassa.integrations.bfdcabinet.DevelopmentIdentity

/**
 * Порты кабинета поверх одного [BfdCabinet]: вошедший у них общий.
 *
 * Экземпляр модуля один на приложение и открыт наружу `data` — через [bfd]
 * его берут соседние адаптеры того же кабинета (аналитика, мастер
 * подключения), чтобы вход владельца был у всех один, а не у каждого свой.
 *
 * @property bfd кабинет БФД, которым говорят все порты этого набора.
 */
class RemoteCabinet(
    val bfd: BfdCabinet,
    override val signer: Signer,
    override val files: SavedFiles,
    override val signing: Signing = Signing.NcaLayerOnly
) : CabinetPorts {
    override val account: CabinetAccount = RemoteAccount(bfd.account)
    override val company: CabinetCompanies = RemoteCompanies(bfd.company)
    override val places: CabinetPlaces = RemotePlaces(bfd.places)
    override val addresses: CabinetAddresses = RemoteAddresses(bfd.addresses)
    override val registers: CabinetRegisters = RemoteRegisters(bfd.registers, bfd.analytics)
    override val applications: CabinetApplications = RemoteApplications(bfd.applications)
    override val cards: CabinetCards = RemoteCards(bfd.cards)
    override val documents: CabinetDocuments = RemoteDocuments(bfd.documents)

    /** Сборка набора: кабинет по адресу рабочего места и подписывающий владельца. */
    companion object {
        /**
         * Кабинет по адресу [address]; подписью владельца [signer] в него входят.
         *
         * @param server IP сервера кабинета — когда его имя в сети не находится;
         *   пусто — имя находит сеть.
         * @param journal куда писать ход обмена: метод, путь, код ответа — без тел.
         * @param developer личность разработчика: задана — кабинет берёт её из
         *   заголовков, а не из подписи. Только явной настройкой машины.
         * @param signing способы подписи и стол подписи; по умолчанию — только NCALayer.
         */
        fun open(
            address: String,
            server: String = "",
            signer: Signer,
            files: SavedFiles,
            journal: Journal,
            developer: DevelopmentIdentity? = null,
            signing: Signing = Signing.NcaLayerOnly
        ): RemoteCabinet {
            if (developer != null) journal.warn("cabinet: developer entry by headers is on, no EDS sign-in")
            val settings = CabinetSettings(
                baseUrl = address,
                serverIp = server.ifBlank { null },
                development = developer
            )
            return RemoteCabinet(BfdCabinet(settings, signing(signer), exchange(journal)), signer, files, signing)
        }
    }
}
