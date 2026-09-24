package kz.mybrain.superkassa.data.local

import kz.mybrain.superkassa.integrations.bfdcabinet.CabinetSettings
import java.io.File

/**
 * Чем рабочее место отвечает на вопрос «где кабинет и кем в него входить».
 *
 * Отдельный предмет от настроек кассы: кабинет — служба владельца, а касса
 * работает в процессе приложения. Адрес кабинета меняется вместе с его
 * площадкой и ни на что в кассе не влияет.
 */
class CabinetPreferences(private val directory: File?) {

    /**
     * Адрес личного кабинета ОФД.
     *
     * Кабинет — отдельная служба: на этой машине он занимает соседний порт,
     * на стенде стоит своим адресом. Зашитый адрес заставил бы пересобирать
     * приложение ради переезда службы.
     */
    var url: String
        get() = readSetting(cabinetFile) ?: CabinetSettings.DEFAULT_URL
        set(value) = writeSetting(cabinetFile, value.trim().takeIf { it.isNotBlank() })

    /** ИИН и БИН для входа в кабинет без ЭЦП; помнятся, чтобы не набирать перед каждым показом. */
    var developerIin: String
        get() = readSetting(developerIinFile) ?: ""
        set(value) = writeSetting(developerIinFile, value.trim().takeIf { it.isNotBlank() })

    var developerBin: String
        get() = readSetting(developerBinFile) ?: ""
        set(value) = writeSetting(developerBinFile, value.trim().takeIf { it.isNotBlank() })

    private val cabinetFile = File(directory, "cabinet")

    private val developerIinFile = File(directory, "cabinet-developer-iin")

    private val developerBinFile = File(directory, "cabinet-developer-bin")
}
