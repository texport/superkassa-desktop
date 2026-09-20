package kz.mybrain.superkassa.desktop.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kz.mybrain.superkassa.desktop.server.Kkm
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.theme.Appearance

/**
 * Что помнит это рабочее место: язык, вид, рельс, колонку точек, окно
 * и свои названия касс.
 *
 * Выбор держится состоянием, а не читается из файла при каждом обращении:
 * файл Compose не наблюдает, и переименованная касса оставалась на экране
 * под прежним названием до перезапуска.
 */
class WorkplaceSettings(private val preferences: Preferences) {

    /**
     * Язык интерфейса.
     *
     * Касса работает в Казахстане, поэтому по умолчанию — государственный.
     * Выбор запоминается: кассир меняет язык один раз, а не каждое утро.
     */
    var language: Language by mutableStateOf(Language.byCode(preferences.language))
        private set

    /** Светлая или тёмная касса: выбор держится этого рабочего места. */
    var appearance: Appearance by mutableStateOf(Appearance.byCode(preferences.appearance))
        private set

    /** Свёрнут ли рельс разделов: подписи спрятаны, значки остались. */
    var railCollapsed: Boolean by mutableStateOf(preferences.railCollapsed)
        private set

    /** Свёрнута ли колонка торговых точек кабинета: список спрятан, касса во всю ширину. */
    var placesCollapsed: Boolean by mutableStateOf(preferences.placesCollapsed)
        private set

    /** Свои названия касс: ключ — касса, значение — как её зовут здесь. */
    private val localNames = mutableStateMapOf<String, String>()

    fun switchLanguage(chosen: Language) {
        language = chosen
        preferences.language = chosen.code
    }

    fun switchAppearance(chosen: Appearance) {
        appearance = chosen
        preferences.appearance = chosen.code
    }

    fun toggleRail() {
        railCollapsed = !railCollapsed
        preferences.railCollapsed = railCollapsed
    }

    fun togglePlaces() {
        placesCollapsed = !placesCollapsed
        preferences.placesCollapsed = placesCollapsed
    }

    /** Размер окна, каким кассир оставил его в прошлый раз. */
    val windowSize: Pair<Int, Int>? get() = preferences.windowSize

    /** Запоминает размер окна: на кассовом столе монитор не меняется. */
    fun rememberWindowSize(width: Int, height: Int) {
        preferences.windowSize = width to height
    }

    /**
     * Как зовут кассу на этом экране.
     *
     * Порядок один на всё приложение: своё название рабочего места —
     * его задали здесь и руками, — затем название с узла, затем
     * регистрационный номер. Своё стоит первым, потому что его дали
     * позже и зная про узловое.
     */
    fun nameOf(kkm: Kkm): String =
        localNames[kkm.kkmId] ?: kkm.name?.takeIf { it.isNotBlank() } ?: kkm.title

    fun rename(kkm: Kkm, name: String?) {
        val chosen = name?.takeIf { it.isNotBlank() }
        preferences.rename(kkm.kkmId, chosen)
        if (chosen == null) localNames.remove(kkm.kkmId) else localNames[kkm.kkmId] = chosen
    }

    /**
     * Поднимает свои названия для только что прочитанного списка касс.
     *
     * До первого обращения к настройкам названий в состоянии нет, а список
     * и вход обязаны показывать кассу так, как её назвали здесь.
     */
    fun adoptNames(loaded: List<Kkm>) {
        loaded.forEach { kkm ->
            preferences.localName(kkm.kkmId)?.let { localNames[kkm.kkmId] = it }
        }
    }

    /**
     * Подбирает кассам узла названия, данные им в кабинете.
     *
     * Название кассе даёт владелец в кабинете, а нужно оно кассиру
     * на входе, когда кабинет закрыт. Отсюда оно уходит на узел —
     * один вход владельца в кабинет называет кассу для всех рабочих мест.
     *
     * Уже названное не трогается: ни своё название рабочего места,
     * ни название, которое у кассы на узле уже есть.
     *
     * @param named пары «касса кабинета — её название».
     * @param loaded кассы, известные узлу: по ним и опознаётся, какая это.
     * @return пары «касса — название» для тех, кто ещё безымянен.
     */
    fun matchCabinetNames(named: Map<String, String>, loaded: List<Kkm>): List<Pair<Kkm, String>> =
        loaded.mapNotNull { kkm ->
            if (localNames.containsKey(kkm.kkmId)) return@mapNotNull null
            if (!kkm.name.isNullOrBlank()) return@mapNotNull null
            val name = kkm.kkmKgdId?.let { named[it] } ?: kkm.ofdSystemId?.let { named[it] }
            name?.takeIf { it.isNotBlank() }?.let { kkm to it }
        }
}
