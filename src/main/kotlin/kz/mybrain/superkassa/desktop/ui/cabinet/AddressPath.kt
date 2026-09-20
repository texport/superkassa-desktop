package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.server.cabinet.AddressSuggestion
import kz.mybrain.superkassa.desktop.server.cabinet.addressBuildings
import kz.mybrain.superkassa.desktop.server.cabinet.addressLocalities
import kz.mybrain.superkassa.desktop.server.cabinet.addressRegions
import kz.mybrain.superkassa.desktop.server.cabinet.addressStreets
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts

/**
 * Что уже выбрано и какого рода следующий шаг.
 *
 * Глубина пунктов не фиксирована: после выбора пункта регистр спрашивается
 * о вложенных, и пока они есть, следующий шаг — снова пункт.
 */
internal class AddressPath {
    val chosen = mutableStateListOf<AddressSuggestion>()
    var building: AddressSuggestion? by mutableStateOf(null)
    private var nestedUnderLast: Boolean by mutableStateOf(false)
    private var probed: Boolean by mutableStateOf(true)

    val depth: Int get() = chosen.size

    /** Известно ли, какого рода следующий шаг: после выбора пункта это решает регистр. */
    val stepKnown: Boolean get() = probed

    fun nestedResolved(nested: Boolean) {
        nestedUnderLast = nested
        probed = true
    }

    private val kind: StepKind
        get() = when {
            chosen.isEmpty() -> StepKind.Region
            chosen.size == 1 || nestedUnderLast -> StepKind.Locality
            chosen.last().level == LEVEL_LOCALITY -> StepKind.Street
            else -> StepKind.Building
        }

    fun currentLabel(texts: CabinetTexts): String = kind.label(texts)

    fun labelAt(at: Int, texts: CabinetTexts): String = when {
        at == 0 -> texts.addressRegion
        chosen[at].level == LEVEL_STREET -> texts.addressStreet
        else -> texts.addressLocality
    }

    suspend fun lookup(cabinet: CabinetSession, token: String, needle: String): List<AddressSuggestion> =
        when (kind) {
            StepKind.Region -> cabinet.client.addressRegions(token, needle).items
            StepKind.Locality -> cabinet.client.addressLocalities(token, chosen.last().id, needle).items
            StepKind.Street -> cabinet.client.addressStreets(token, chosen.last().id, needle).items
            StepKind.Building -> cabinet.client.addressBuildings(token, chosen.last().id, needle).items
        }

    /** Дом завершает путь; за пунктом следующий шаг известен только после ответа регистра о вложенных. */
    fun choose(suggestion: AddressSuggestion) {
        if (kind == StepKind.Building) {
            building = suggestion
            return
        }
        chosen.add(suggestion)
        nestedUnderLast = false
        probed = suggestion.level != LEVEL_LOCALITY
    }

    fun dropFrom(at: Int) {
        while (chosen.size > at) chosen.removeAt(chosen.size - 1)
        nestedUnderLast = false
        probed = true
        building = null
    }

    fun reset() = dropFrom(0)
}

private enum class StepKind(val label: (CabinetTexts) -> String) {
    Region({ it.addressRegion }),
    Locality({ it.addressLocality }),
    Street({ it.addressStreet }),
    Building({ it.addressBuilding })
}

internal const val LEVEL_LOCALITY = "LOCALITY"
internal const val LEVEL_STREET = "STREET"
