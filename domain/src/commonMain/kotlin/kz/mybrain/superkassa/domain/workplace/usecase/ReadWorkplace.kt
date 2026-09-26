package kz.mybrain.superkassa.domain.workplace.usecase

import kz.mybrain.superkassa.domain.map.model.MapProvider
import kz.mybrain.superkassa.domain.workplace.model.MapServices
import kz.mybrain.superkassa.domain.workplace.port.WorkplaceChoices
import kz.mybrain.superkassa.domain.workplace.port.WorkplaceMemory

/** Что записано на этой машине: адрес кабинета, службы карты и отрасль кассы. */
class ReadWorkplace(private val choices: WorkplaceChoices, private val memory: WorkplaceMemory) {

    /**
     * Настройки машины, как они сохранены.
     *
     * @property cabinetServer IP сервера кабинета; пусто — имя находит сеть.
     * @property publicMaps общедоступные службы карты — на месте пустых полей.
     * @property domainCode вид отрасли кассы; `null` — торговля или касса не выбрана.
     * @property mapProviders поставщики плиток карты на выбор; первый — по умолчанию.
     */
    data class Saved(
        val cabinetUrl: String,
        val cabinetServer: String,
        val maps: MapServices,
        val publicMaps: MapServices,
        val domainCode: String?,
        val mapProviders: List<MapProvider> = emptyList()
    )

    /** @param kkmId касса, чья отрасль нужна; `null` — касса не выбрана. */
    operator fun invoke(kkmId: String?): Saved =
        Saved(
            cabinetUrl = choices.cabinetUrl,
            cabinetServer = choices.cabinetServer,
            maps = choices.maps,
            publicMaps = choices.publicMaps,
            domainCode = kkmId?.let(memory::domain),
            mapProviders = choices.mapProviders
        )
}
