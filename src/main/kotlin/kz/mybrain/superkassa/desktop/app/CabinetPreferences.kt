package kz.mybrain.superkassa.desktop.app

import kz.mybrain.superkassa.desktop.server.cabinet.CabinetClient
import java.io.File

/**
 * Чем рабочее место отвечает на вопрос «где кабинет и кем в него входить».
 *
 * Отдельный предмет от настроек узла: кабинет — служба владельца, а узел —
 * служба кассы. Они стоят на разных машинах, переезжают порознь, и общий
 * адрес на двоих однажды увёл бы кассу вслед за кабинетом.
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
        get() = readSetting(cabinetFile) ?: CabinetClient.DEFAULT_URL
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
