package kz.mybrain.superkassa.domain.settings.usecase.core

import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmListParams
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.model.ask
import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.settings.model.KassaFacts
import kz.mybrain.superkassa.domain.settings.port.CoreSettingsStore
import kz.mybrain.superkassa.domain.version.model.AppVersion
import kz.mybrain.superkassa.domain.version.model.BuildVersion

/**
 * Сведения о кассе на этой машине — без пина и без выбранной кассы.
 *
 * Версии известны сборке и есть всегда. Число касс спрашивается у кассы:
 * не ответила — число не показывается, а остальные сведения остаются,
 * ради них поддержка и открыла карточку.
 */
class ReadKassaFacts(private val store: CoreSettingsStore, private val kassa: Kassa) {

    suspend operator fun invoke(): KassaFacts {
        val count = kassa.ask { it.listKkms(KkmListParams(limit = 1)).total }
        return KassaFacts(
            appVersion = AppVersion.current.label,
            coreVersion = BuildVersion.CORE,
            directory = store.directory,
            kkmCount = (count as? Answer.Done)?.value
        )
    }
}
