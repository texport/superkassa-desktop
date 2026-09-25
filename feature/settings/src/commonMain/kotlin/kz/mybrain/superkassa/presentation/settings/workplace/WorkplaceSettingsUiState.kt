package kz.mybrain.superkassa.presentation.settings.workplace

import kz.mybrain.superkassa.domain.workplace.model.MapServices
import kz.mybrain.superkassa.domain.workplace.model.ServiceAddress
import kz.mybrain.superkassa.domain.workplace.model.custom
import kz.mybrain.superkassa.domain.workplace.model.tidy

/**
 * Настройки этой машины: адрес кабинета, службы карты, отрасль кассы.
 *
 * Набранное переживает уход в другой раздел: модель живёт окном, а экран
 * настроек уходит из состава вместе с разделом, и поля забывали набранное
 * молча. На диск попадает только сохранённое кнопкой.
 *
 * @property cabinetDraft набранный адрес кабинета; `null` — поле не трогали.
 * @property cabinetServer IP сервера кабинета; пусто — имя находит сеть.
 * @property serverDraft набранный IP сервера; `null` — поле не трогали.
 * @property mapDrafts набранные адреса служб карты; `null` — не трогали.
 * @property publicMaps общедоступные службы: видны подсказкой в пустых полях.
 * @property domainCode вид отрасли выбранной кассы; `null` — торговля.
 */
data class WorkplaceSettingsUiState(
    val kkmId: String? = null,
    val cabinetUrl: String = "",
    val cabinetDraft: String? = null,
    val cabinetServer: String = "",
    val serverDraft: String? = null,
    val maps: MapServices = MapServices(),
    val mapDrafts: MapServices? = null,
    val publicMaps: MapServices = MapServices(),
    val domainCode: String? = null
) {
    /** Адрес кабинета в поле: набранный, иначе сохранённый. */
    val cabinetField: String get() = cabinetDraft ?: cabinetUrl

    /** Годен ли набранный адрес и отличается ли он от сохранённого. */
    val cabinetChanged: Boolean get() = ServiceAddress.changed(cabinetField, cabinetUrl)

    /** IP сервера в поле: набранный, иначе сохранённый. */
    val serverField: String get() = serverDraft ?: cabinetServer

    /** Набранный IP сервера отличается от сохранённого. */
    val serverChanged: Boolean get() = serverField.trim() != cabinetServer

    /** Адрес без схемы назван до сохранения, а не отказом входа в кабинет. */
    val cabinetMalformed: Boolean get() = cabinetField.isNotBlank() && !ServiceAddress.valid(cabinetField)

    /** Адреса карты в полях: набранные, иначе сохранённые. */
    val mapFields: MapServices get() = mapDrafts ?: maps

    /** Сохранять есть что: набранное отличается от записанного. */
    val mapsChanged: Boolean get() = mapFields.tidy() != maps

    /** Хоть одна служба своя: возвращать к общедоступным есть что. */
    val mapsCustom: Boolean get() = mapFields.custom
}
