package kz.mybrain.superkassa.domain.workplace.port

import kz.mybrain.superkassa.domain.workplace.model.MapServices

/**
 * Настройки этой машины, которые владелец задаёт в «Настройках».
 *
 * Хранит их рабочее место, а не касса: адрес кабинета и службы карты
 * у каждой машины свои, вид отрасли и своё название кассы — тоже.
 * Читаются отрасль и название там же, откуда их читает продажа, —
 * в [WorkplaceMemory]; здесь только запись.
 */
interface WorkplaceChoices {

    /** Адрес личного кабинета БФД. */
    var cabinetUrl: String

    /** Службы карты; пустое поле — общедоступная служба сообщества. */
    var maps: MapServices

    /** Общедоступные службы, которыми карта работает там, где поле пусто. */
    val publicMaps: MapServices

    /** Вид отрасли кассы, как его называет ядро; `null` — торговля. */
    fun chooseDomain(kkmId: String, code: String?)

    /**
     * Своё название кассы на этой машине; `null` снимает его.
     *
     * Запасной путь, а не второе место хранения: название уходит в кассу,
     * а здесь остаётся только то, что касса не приняла.
     */
    fun rename(kkmId: String, name: String?)
}
