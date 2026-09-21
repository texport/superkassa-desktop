package kz.mybrain.superkassa.desktop

/**
 * Ответы кабинета для снимков — теми же телами, что ходят по сети.
 *
 * Собраны в одном месте, а не разложены по проверкам: одно и то же тело
 * нужно и снимку списка, и снимку раскрытой карточки, а сдвинется схема
 * кабинета — править придётся один файл. Значения взяты по образцу
 * `CabinetContractTest` и `CabinetDocumentsTest`, где лежат живые ответы.
 */
internal object CabinetBodies {

    /** Один чек в списке: с него раскрывают карточку. */
    const val ONE_RECEIPT = """{"page":0,"size":50,"totalElements":1,"items":[
        {"transactionId":"t-1","receiptNumber":"12","shiftNumber":3,"operationType":"SALE",
         "total":435.84,"createdAt":"2026-09-07T19:02:59Z","deliveryStatus":"ONLINE_OK",
         "kgdMark":"210000000001"}]}"""

    /** Пустая страница любого вида документов. */
    const val NOTHING = """{"page":0,"size":50,"totalElements":0,"items":[]}"""

    /** Сколько чего накопила касса: счёт за всё время, а не за выбранный срок. */
    const val OVERVIEW = """{"cashRegisterId":"r-1","receiptsCount":100,"reportsCount":7,
        "shiftsCount":5,"cashMovementsCount":3,
        "registrationCard":{"available":true,"status":"REGISTERED","updatedAt":"2026-09-18T09:12:00Z"}}"""

    /** Открытая смена и закрытая: у открытой итогов ещё нет. */
    const val SHIFTS = """{"page":0,"size":50,"totalElements":2,"items":[
        {"shiftNumber":4,"status":"OPEN","openedAt":"2026-09-21T09:00:00Z"},
        {"shiftNumber":3,"status":"CLOSED","openedAt":"2026-09-07T10:05:06Z",
         "closedAt":"2026-09-07T15:05:31Z","receiptsCount":12,"saleTotal":3570.00,
         "returnTotal":1900.00,"buyTotal":400.00,"cashBalance":6070.00}]}"""

    const val REPORTS = """{"page":0,"size":50,"totalElements":2,"items":[
        {"transactionId":"z-1","reportType":"Z","shiftNumber":3,"createdAt":"2026-09-07T15:05:31Z",
         "saleTotal":3570.00,"deliveryStatus":"ONLINE_OK"},
        {"transactionId":"x-1","reportType":"X","shiftNumber":3,"createdAt":"2026-09-07T13:00:00Z",
         "saleTotal":1200.00,"deliveryStatus":"ONLINE_OK"}]}"""

    const val MOVEMENTS = """{"page":0,"size":50,"totalElements":2,"items":[
        {"transactionId":"m-1","movementType":"DEPOSIT","amount":5000.00,"shiftNumber":3,
         "createdAt":"2026-09-07T10:06:00Z","sendStatus":"ACCEPTED"},
        {"transactionId":"m-2","movementType":"WITHDRAWAL","amount":1500.00,"shiftNumber":3,
         "createdAt":"2026-09-07T14:40:00Z","sendStatus":"ACCEPTED"}]}"""

    /** Чек, дошедший до БФД: с отметкой КГД и с длинным наименованием позиции. */
    const val RECEIPT_MARKED = """{"transactionId":"t-1","receiptNumber":"12","shiftNumber":3,
        "operationType":"SALE","total":435.84,"taxTotal":60.12,"cashTotal":435.84,"cardTotal":0,
        "createdAt":"2026-09-07T19:02:59Z","registrationNumber":"000000010001",
        "operator":{"code":1,"name":"Администратор"},
        "items":[{"positionNumber":1,"name":"Кофе молотый «Эфиопия Иргачеффе» 250 г","quantity":3.0,
                  "price":150.55,"amount":435.84,"taxPercent":16,"taxAmount":60.12}],
        "deliveryStatus":"ONLINE_OK","kgdMark":"210000000001","kgdMarkAt":"2026-09-07T19:03:01Z",
        "protocolDocumentId":"3846668294"}"""

    /** Тот же чек, до БФД не доехавший: отметки КГД нет вовсе. */
    const val RECEIPT_UNMARKED = """{"transactionId":"t-1","receiptNumber":"12","shiftNumber":3,
        "operationType":"SALE","total":435.84,"taxTotal":60.12,"cashTotal":435.84,
        "createdAt":"2026-09-07T19:02:59Z","operator":{"code":1,"name":"Администратор"},
        "items":[{"positionNumber":1,"name":"Кофе","quantity":3.0,"price":150.55,"amount":435.84,
                  "taxPercent":16,"taxAmount":60.12}],
        "deliveryStatus":"OFFLINE","protocolDocumentId":"3846668294"}"""

    /** Действующая регистрационная карта кассы. */
    const val CARD = """{"cashRegisterId":"r-1","status":"REGISTERED","companyBin":"230140000000",
        "companyName":"ТОО «Азик и Ко»","retailPlaceName":"Магазин на Абая",
        "address":"Алматы, Алмалинский, Абая, 1","rka":"0202247079279855","cato":"751110000",
        "registrationNumber":"000000010001","updatedAt":"2026-09-18T09:12:00Z"}"""

    /** Две версии карты: действующая после перерегистрации и закрытая ею прежняя. */
    const val CARD_VERSIONS = """[
        {"versionNumber":2,"status":"REGISTERED","openedAt":"2026-09-18T09:12:00Z","closedAt":null,
         "openedByActionType":"REREGISTRATION","changedFields":["retailPlace","address"],"active":true},
        {"versionNumber":1,"status":"REGISTERED","openedAt":"2026-05-02T11:00:00Z",
         "closedAt":"2026-09-18T09:12:00Z","openedByActionType":"REGISTRATION",
         "closedByActionType":"REREGISTRATION","changedFields":[],"active":false}]"""

    /** Одна торговая точка компании: её выбирает заявление о перерегистрации. */
    const val PLACES = """{"page":0,"size":50,"totalElements":1,"items":[
        {"id":"p-1","name":"Магазин на Абая","addressRef":"0202247079279855","cato":"751110000",
         "address":"Алматы, Алмалинский, Абая, 1","latitude":43.238949,"longitude":76.889709}]}"""
}
