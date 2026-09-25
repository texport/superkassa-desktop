package kz.mybrain.superkassa.domain.settings.usecase.core

import io.github.texport.superkassa.core.domain.api.model.settings.CoreSettings
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.model.answering
import kz.mybrain.superkassa.domain.settings.port.CoreSettingsStore

/** Действующие настройки кассы на этой машине. */
class ReadCoreSettings(private val store: CoreSettingsStore) {

    suspend operator fun invoke(): Answer<CoreSettings> = answering { store.read() }
}
