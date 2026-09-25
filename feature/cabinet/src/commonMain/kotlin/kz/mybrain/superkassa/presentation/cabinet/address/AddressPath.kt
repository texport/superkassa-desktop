package kz.mybrain.superkassa.presentation.cabinet.address

import kz.mybrain.superkassa.domain.cabinet.model.AddressLevel
import kz.mybrain.superkassa.domain.cabinet.model.AddressSuggestion
import kz.mybrain.superkassa.strings.api.cabinet.CabinetTexts

/**
 * Что уже выбрано в адресном регистре и какого рода следующий шаг.
 *
 * Глубина пунктов не фиксирована: после выбора пункта регистр спрашивается
 * о вложенных, и пока они есть, следующий шаг — снова пункт.
 *
 * @property probed известно ли, какого рода следующий шаг: после выбора
 *   пункта это решает регистр.
 * @property query что набрано в поле текущего шага.
 * @property found что регистр нашёл по набранному.
 * @property searched регистр уже ответил на этом шаге: пустой ответ
 *   говорится и на пустом запросе, иначе молчащий регистр не отличить
 *   от полного.
 */
internal data class AddressPath(
    val chosen: List<AddressSuggestion> = emptyList(),
    val building: AddressSuggestion? = null,
    val nestedUnderLast: Boolean = false,
    val probed: Boolean = true,
    val query: String = "",
    val found: List<AddressSuggestion> = emptyList(),
    val searched: Boolean = false
) {
    val depth: Int get() = chosen.size

    /** Какой шаг регистра следующий. */
    val level: AddressLevel
        get() = when {
            chosen.isEmpty() -> AddressLevel.Region
            chosen.size == 1 || nestedUnderLast -> AddressLevel.Locality
            chosen.last().level == LEVEL_LOCALITY -> AddressLevel.Street
            else -> AddressLevel.Building
        }

    /** Показывать ли поле следующего шага: дом не выбран и род шага известен. */
    val stepShown: Boolean get() = building == null && probed

    fun currentLabel(texts: CabinetTexts): String = level.label(texts)

    fun labelAt(at: Int, texts: CabinetTexts): String = when {
        at == 0 -> texts.addressRegion
        chosen[at].level == LEVEL_STREET -> texts.addressStreet
        else -> texts.addressLocality
    }

    /** Дом завершает путь; за пунктом следующий шаг известен только после ответа регистра о вложенных. */
    fun choose(suggestion: AddressSuggestion): AddressPath {
        if (level == AddressLevel.Building) return copy(building = suggestion)
        return AddressPath(chosen = chosen + suggestion, probed = suggestion.level != LEVEL_LOCALITY)
    }

    /** Снимает выбранное с уровня [at] и ниже. */
    fun dropFrom(at: Int): AddressPath = AddressPath(chosen = chosen.take(at))
}

/** Как шаг регистра назван в поле. */
private fun AddressLevel.label(texts: CabinetTexts): String = when (this) {
    AddressLevel.Region -> texts.addressRegion
    AddressLevel.Locality -> texts.addressLocality
    AddressLevel.Street -> texts.addressStreet
    AddressLevel.Building -> texts.addressBuilding
}

internal const val LEVEL_LOCALITY = "LOCALITY"
internal const val LEVEL_STREET = "STREET"
