package kz.mybrain.superkassa.domain.cabinet.port

/**
 * Всё, что кабинет даёт экранам, — одним набором портов.
 *
 * Набор, а не один интерфейс на все ручки: компания, точки, кассы,
 * заявления, карты и документы меняются независимо, и экрану точек
 * незачем знать про документы. Собирается в точке сборки приложения
 * из реализаций `data` и отдаётся моделям кабинета, аналитики и мастера.
 */
interface CabinetPorts {
    val account: CabinetAccount
    val company: CabinetCompanies
    val places: CabinetPlaces
    val addresses: CabinetAddresses
    val registers: CabinetRegisters
    val applications: CabinetApplications
    val cards: CabinetCards
    val documents: CabinetDocuments
    val signer: Signer

    /**
     * Чем подписывать и что подписывающий спрашивает у владельца. По умолчанию —
     * один NCALayer: окно подписи он показывает сам, спрашивать кассе нечего.
     */
    val signing: Signing get() = Signing.NcaLayerOnly

    /** Куда владелец сохраняет PDF регистрационной карты. */
    val files: SavedFiles
}
