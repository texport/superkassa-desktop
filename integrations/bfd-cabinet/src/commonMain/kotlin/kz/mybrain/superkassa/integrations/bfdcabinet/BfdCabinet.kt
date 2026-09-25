package kz.mybrain.superkassa.integrations.bfdcabinet

import io.ktor.client.engine.HttpClientEngine
import kz.mybrain.superkassa.integrations.bfdcabinet.address.AddressApi
import kz.mybrain.superkassa.integrations.bfdcabinet.analytics.AnalyticsApi
import kz.mybrain.superkassa.integrations.bfdcabinet.applications.ApplicationsApi
import kz.mybrain.superkassa.integrations.bfdcabinet.company.CompanyApi
import kz.mybrain.superkassa.integrations.bfdcabinet.documents.DocumentsApi
import kz.mybrain.superkassa.integrations.bfdcabinet.places.PlacesApi
import kz.mybrain.superkassa.integrations.bfdcabinet.register.CardsApi
import kz.mybrain.superkassa.integrations.bfdcabinet.register.RegistersApi
import kz.mybrain.superkassa.integrations.bfdcabinet.signin.AccountApi
import kz.mybrain.superkassa.integrations.bfdcabinet.signin.CabinetAccess
import kz.mybrain.superkassa.integrations.bfdcabinet.transport.CabinetHttp
import kz.mybrain.superkassa.integrations.bfdcabinet.transport.CabinetLink

/**
 * Кабинет БФД (ECC) от имени одного владельца.
 *
 * Одна связь на приложение: вошедший у всех сценариев общий. Каждый сценарий —
 * своим набором ручек, путь каждой написан в модуле один раз. Все обращения,
 * кроме входа, идут от имени вошедшего: доступа нет или он истёк —
 * [CabinetExpired].
 *
 * Неудачи — [CabinetFailure]: отказ кабинета его кодом и словами, непонятный
 * ответ, конец сеанса, молчание кабинета.
 *
 * @param settings адрес кабинета, сроки и режим разработки.
 * @param signer кто подписывает задачу входа ключом владельца.
 * @param journal куда писать ход обмена.
 * @param engine движок Ktor; по умолчанию — движок платформы.
 */
class BfdCabinet(
    settings: CabinetSettings = CabinetSettings(),
    signer: CabinetSigner,
    journal: CabinetJournal = CabinetJournal.Silent,
    engine: HttpClientEngine = platformEngine(settings.pin)
) : AutoCloseable {
    private val http = CabinetHttp(settings, journal, engine)
    private val link = CabinetLink(http, CabinetAccess())

    /** Вход по ЭЦП, выход и вошедший. */
    val account: AccountApi = AccountApi(http, link.access, signer, settings.development != null)

    /** Карточка компании и классификатор ОКЭД. */
    val company: CompanyApi = CompanyApi(link)

    /** Адресный регистр. */
    val addresses: AddressApi = AddressApi(link)

    /** Торговые точки. */
    val places: PlacesApi = PlacesApi(link)

    /** Кассы, их состояние, токен и справочник моделей. */
    val registers: RegistersApi = RegistersApi(link, settings)

    /** Регистрационные карты касс. */
    val cards: CardsApi = CardsApi(link)

    /** Заявления в ИСНА. */
    val applications: ApplicationsApi = ApplicationsApi(link)

    /** Фискальные документы касс. */
    val documents: DocumentsApi = DocumentsApi(link)

    /** Аналитика: карта касс, адреса обмена, торговая сводка. */
    val analytics: AnalyticsApi = AnalyticsApi(link)

    /** Закрывает соединения с кабинетом. */
    override fun close() = http.close()
}
