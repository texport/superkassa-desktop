package kz.mybrain.superkassa.presentation.cabinet

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kz.mybrain.superkassa.domain.cabinet.model.CabinetOwner
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.domain.cabinet.model.RetailPlace
import kotlin.time.TimeMark

/**
 * Кабинет окна: кто вошёл, хозяйство компании и чем кабинет занят.
 *
 * Списки точек и касс одни на окно: заявление о перерегистрации выбирает
 * точку из того же списка, что и колонка, и только что созданная точка
 * видна везде сразу.
 *
 * @property signingSince с какого мгновения идёт вход по ЭЦП: экран
 *   отсчитывает от него срок подписи; `null` — вход не идёт.
 * @property placesTotal сколько точек у компании по словам кабинета:
 *   пока список читается, прочитано меньше, и колонка говорит «Показано
 *   50 из 2000», а не выдаёт первую страницу за всё хозяйство.
 * @property placesRead кабинет ответил на чтение точек — удачей или отказом;
 *   до ответа колонка ждёт, а не объявляет хозяйство пустым.
 * @property placesTrouble почему точки не прочитаны, словами владельца.
 * @property blocked какие кассы кабинет считает заблокированными; `null` —
 *   не спрашивали или ответа не было, и это не то же, что «таких нет».
 * @property running сколько начатых владельцем обращений идёт сейчас.
 * @property documentsOf чьи документы открыты поверх кабинета.
 */
data class CabinetUiState(
    val owner: CabinetOwner? = null,
    val address: String = "",
    val signingSince: TimeMark? = null,
    val places: List<RetailPlace> = emptyList(),
    val placesTotal: Int = 0,
    val placesRead: Boolean = false,
    val placesTrouble: String? = null,
    val registers: List<CabinetRegister> = emptyList(),
    val blocked: Set<String>? = null,
    val running: Int = 0,
    val documentsOf: CabinetRegister? = null
) {
    /** Вошёл ли владелец. */
    val open: Boolean get() = owner != null

    /**
     * Отметка вошедшего для разделов, которые перечитывают своё при смене
     * владельца; `null` — владелец не входил. Сам доступ модуль кабинета
     * наружу не отдаёт: отметка лишь различает, кто вошёл.
     */
    val access: String? get() = owner?.let { "${it.user.id}/${it.company.id}" }

    /** Кабинет занят тем, что начал владелец: кнопки не принимают второго нажатия. */
    val busy: Boolean get() = running > 0

    /** Сколько точек показать итогом: прочитанное не меньше объявленного. */
    val shownTotal: Int get() = maxOf(places.size, placesTotal)
}

/**
 * Что открыто в разделе кабинета: вкладка и документы кассы.
 *
 * Живёт в модели кабинета, а не в разметке: вкладка переживает и шаги
 * истории окна, и уход в другой раздел — владелец возвращается туда,
 * откуда ушёл, а не на вкладку компании.
 */
class CabinetView internal constructor(private val screen: MutableStateFlow<CabinetUiState>) {
    private val chosen = MutableStateFlow(CabinetTab.Company)

    /** Открытая вкладка. */
    internal val tab: StateFlow<CabinetTab> = chosen.asStateFlow()

    internal fun selectTab(tab: CabinetTab) {
        chosen.value = tab
    }

    /** Открывает документы кассы поверх кабинета: выбранная касса остаётся выбранной. */
    fun openDocuments(register: CabinetRegister) = screen.update { it.copy(documentsOf = register) }

    fun closeDocuments() = screen.update { it.copy(documentsOf = null) }
}
