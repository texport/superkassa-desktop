package kz.mybrain.superkassa.presentation.cabinet.enroll

import kz.mybrain.superkassa.domain.cabinet.model.KkmModel
import kz.mybrain.superkassa.domain.cabinet.model.RetailPlace

/**
 * Справочники формы заведения кассы.
 *
 * @property models модели касс из справочника ИСНА: читаются один раз на окно.
 * @property places точки, найденные кабинетом по набранному: одна страница поиска.
 */
internal data class AddRegisterUiState(
    val models: List<KkmModel> = emptyList(),
    val places: List<RetailPlace> = emptyList()
)
