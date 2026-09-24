package kz.mybrain.superkassa.presentation.settings

import kz.mybrain.superkassa.domain.debug.port.LogBook
import kz.mybrain.superkassa.domain.print.port.PrintChoices
import kz.mybrain.superkassa.domain.print.port.PrintOut
import kz.mybrain.superkassa.domain.settings.port.CoreSettingsStore
import kz.mybrain.superkassa.domain.update.port.Releases
import kz.mybrain.superkassa.domain.update.port.UpdateMemory
import kz.mybrain.superkassa.domain.workplace.port.WorkplaceChoices

/**
 * Порты области «Настройки, печать, обновление» одним набором.
 *
 * В контейнере зависимостей у каждой области одно поле: так контейнер
 * не разрастается с каждым портом, а правки областей не пересекаются.
 *
 * @property logBook журнал рабочего места для окна отладки и режима отладки.
 * @property releases служба выпусков кассы.
 * @property updateMemory что рабочее место помнит о проверке выпусков.
 * @property printOut принтер и диск этой машины для печатной формы.
 * @property printChoices принтер кассы, копии и вид файла.
 * @property coreSettings настройки кассы на этой машине.
 * @property workplace настройки самой машины: кабинет, карта, отрасль, названия.
 */
data class SettingsPorts(
    val logBook: LogBook,
    val releases: Releases,
    val updateMemory: UpdateMemory,
    val printOut: PrintOut,
    val printChoices: PrintChoices,
    val coreSettings: CoreSettingsStore,
    val workplace: WorkplaceChoices
)
