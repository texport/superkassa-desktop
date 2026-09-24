package kz.mybrain.superkassa.domain.settings.port

import kz.mybrain.superkassa.domain.workplace.port.WorkplaceChoices

/**
 * Что настройкам нужно снаружи сверх кассы: настройки кассы на машине и самой машины.
 *
 * Печать, выпуски и журнал отладки стоят среди настроек карточками своих
 * областей и порты берут свои — настройки о них не знают.
 *
 * @property coreSettings настройки кассы на этой машине.
 * @property workplace настройки самой машины: кабинет, карта, отрасль, названия.
 */
data class SettingsPorts(val coreSettings: CoreSettingsStore, val workplace: WorkplaceChoices)
