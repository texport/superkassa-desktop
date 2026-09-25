package kz.mybrain.superkassa.presentation.settings.workplace

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kz.mybrain.superkassa.domain.workplace.model.MapServices
import kz.mybrain.superkassa.presentation.common.model.follow

/** Что владелец меняет в настройках машины. Пустые действия — для снимков вида. */
interface WorkplaceSettingsActions {
    fun typeCabinet(text: String) = Unit

    fun saveCabinet() = Unit

    fun typeMaps(maps: MapServices) = Unit

    fun saveMaps() = Unit

    fun resetMaps() = Unit

    fun chooseDomain(code: String?) = Unit
}

/** Настройки этой машины: пишутся сразу в память рабочего места. */
internal class WorkplaceSettingsViewModel(private val cases: WorkplaceCases) : ViewModel(), WorkplaceSettingsActions {
    private val screen = MutableStateFlow(
        cases.read(null).let { saved ->
            WorkplaceSettingsUiState(null, saved.cabinetUrl, maps = saved.maps, publicMaps = saved.publicMaps)
        }
    )

    val state: StateFlow<WorkplaceSettingsUiState> = screen.asStateFlow()

    init {
        follow(cases.observe().map { it.kkm?.kkmId }.distinctUntilChanged()) { kkmId ->
            screen.update { it.copy(kkmId = kkmId, domainCode = cases.read(kkmId).domainCode) }
        }
    }

    override fun typeCabinet(text: String) = screen.update { it.copy(cabinetDraft = text) }

    /** Новый адрес берётся при следующем входе в кабинет, а не под открытым сеансом. */
    override fun saveCabinet() {
        val saved = cases.saveCabinet(screen.value.cabinetField) ?: return
        screen.update { it.copy(cabinetUrl = saved, cabinetDraft = null) }
    }

    override fun typeMaps(maps: MapServices) = screen.update { it.copy(mapDrafts = maps) }

    override fun saveMaps() {
        val saved = cases.saveMaps(screen.value.mapFields)
        screen.update { it.copy(maps = saved, mapDrafts = null) }
    }

    /** Пустые поля — общедоступные службы сообщества. */
    override fun resetMaps() {
        typeMaps(MapServices())
        saveMaps()
    }

    override fun chooseDomain(code: String?) {
        val kkmId = screen.value.kkmId ?: return
        cases.chooseDomain(kkmId, code)
        screen.update { it.copy(domainCode = code) }
    }
}
