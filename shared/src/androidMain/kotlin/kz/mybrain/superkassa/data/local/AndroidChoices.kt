package kz.mybrain.superkassa.data.local

import kz.mybrain.superkassa.data.map.publicMaps
import kz.mybrain.superkassa.domain.workplace.model.MapServices
import kz.mybrain.superkassa.domain.workplace.port.WorkplaceChoices

/**
 * Настройки машины на Android — в настройках приложения, как и прочая
 * память рабочего места.
 *
 * Отрасль и своё название кассы пишутся туда, откуда их читает продажа;
 * адрес кабинета и службы карты — рядом. Раньше последние помнились до
 * закрытия приложения, и владелец задавал их заново после каждого
 * перезапуска телефона.
 */
class AndroidChoices(private val workplace: AndroidWorkplace) : WorkplaceChoices {

    override var cabinetUrl: String
        get() = workplace.cabinetUrl
        set(value) {
            workplace.cabinetUrl = value
        }

    override var maps: MapServices
        get() = workplace.maps
        set(value) {
            workplace.maps = value
        }

    override val publicMaps: MapServices = publicMaps()

    override fun chooseDomain(kkmId: String, code: String?) = workplace.chooseDomain(kkmId, code)

    override fun rename(kkmId: String, name: String?) = workplace.rename(kkmId, name)
}
