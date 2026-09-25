package kz.mybrain.superkassa.presentation.settings.kkm

import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import kz.mybrain.superkassa.domain.kkm.model.displayName
import kz.mybrain.superkassa.domain.kkm.model.isProgramming
import kz.mybrain.superkassa.domain.kkm.model.title

/**
 * Касса, настройки которой открыты, и её название.
 *
 * @property kkm выбранная касса; `null` — настройки открыты до входа.
 * @property admin права администратора: служебные настройки видит только он.
 * @property localName своё название кассы на этой машине.
 * @property nameDraft набранное в поле названия; `null` — поле не трогали.
 * @property busy касса выполняет команду: кнопки гаснут, второе нажатие
 *   отправляло бы то же самое второй раз.
 * @property decommissionAsked открыт вопрос о снятии кассы.
 */
data class KkmSettingsUiState(
    val kkm: KkmResponse? = null,
    val admin: Boolean = false,
    val localName: String? = null,
    val nameDraft: String? = null,
    val busy: Boolean = false,
    val decommissionAsked: Boolean = false
) {
    /** Как зовут кассу на этой машине. */
    val displayName: String get() = kkm?.displayName(localName).orEmpty()

    /**
     * Что стоит в поле названия.
     *
     * Набранное, иначе данное кассе имя; касса, которую зовут регистрационным
     * номером, названия не имеет, и номер в поле выглядел бы названием.
     */
    val nameField: String get() = nameDraft ?: displayName.takeIf { it != kkm?.title }.orEmpty()

    /** Касса в режиме программирования: настройки меняются только в нём. */
    val programming: Boolean get() = kkm?.isProgramming == true
}
