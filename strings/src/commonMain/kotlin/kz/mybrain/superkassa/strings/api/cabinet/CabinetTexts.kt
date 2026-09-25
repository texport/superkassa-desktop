package kz.mybrain.superkassa.strings.api.cabinet

import kz.mybrain.superkassa.strings.api.cabinet.address.AddressTexts
import kz.mybrain.superkassa.strings.api.cabinet.applications.ApplicationTexts
import kz.mybrain.superkassa.strings.api.cabinet.company.CompanyTexts
import kz.mybrain.superkassa.strings.api.cabinet.documents.DocumentTexts
import kz.mybrain.superkassa.strings.api.cabinet.eds.EdsTexts
import kz.mybrain.superkassa.strings.api.cabinet.enroll.EnrollTexts
import kz.mybrain.superkassa.strings.api.cabinet.machine.MachineTexts
import kz.mybrain.superkassa.strings.api.cabinet.places.PlaceTexts
import kz.mybrain.superkassa.strings.api.cabinet.refusal.CabinetRefusalTexts
import kz.mybrain.superkassa.strings.api.cabinet.register.RegisterTexts
import kz.mybrain.superkassa.strings.api.cabinet.signin.SignInTexts
import kz.mybrain.superkassa.strings.api.map.MapTexts

/**
 * Надписи области «Личный кабинет БФД».
 *
 * Кабинет — рабочее место владельца, а не кассира: здесь заводят кассы,
 * подают заявления в ИСНА и смотрят, что доехало до БФД. Слова взяты
 * из речи владельца и из формулировок КГД, а не из имён полей контракта.
 *
 * Надписи разложены по сценариям кабинета так же, как его экраны: вход,
 * точки, касса, заявления, документы. Здесь остаются только слова,
 * общие для всех сценариев, и наборы, которые берут несколько экранов
 * сразу: названия состояний, подсказки разделов, карта, подпись и работа
 * кассы на этой машине.
 */
data class CabinetTexts(
    val title: String,
    val refresh: String,
    val add: String,
    val remove: String,
    val save: String,
    val close: String,
    val required: String,
    val optional: String,
    val missing: String,
    /** Вход по ЭЦП. */
    val signin: SignInTexts,
    /** Отказы кабинета и подписи — на любом его экране. */
    val refusal: CabinetRefusalTexts,
    /** Компания и её виды деятельности. */
    val company: CompanyTexts,
    /** Торговые точки. */
    val places: PlaceTexts,
    /** Адрес точки по адресному регистру. */
    val address: AddressTexts,
    /** Создание кассы. */
    val enroll: EnrollTexts,
    /** Паспорт кассы, её состояние и регистрационная карта. */
    val register: RegisterTexts,
    /** Заявления в КГД. */
    val applications: ApplicationTexts,
    /** Документы кассы, принятые БФД. */
    val documents: DocumentTexts,
    /** Названия состояний учёта, работы и заявлений: их берут и аналитика, и общее экранов. */
    val statuses: CabinetStatusTexts,
    /** Надписи выбора точки на карте. */
    val map: MapTexts,
    /** Подсказки разделов кабинета: что это за раздел и зачем он владельцу. */
    val hints: CabinetHintTexts,
    /** Ожидание подписи ЭЦП: его показывают и вход, и заявления. */
    val eds: EdsTexts,
    /** Работа кассы на этой машине: её показывают паспорт кассы и заведение кассы кабинета здесь. */
    val machine: MachineTexts
)
