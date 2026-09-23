package kz.mybrain.superkassa.presentation.session

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import kz.mybrain.superkassa.data.node.Dictionary
import kz.mybrain.superkassa.data.node.DictionaryEntry
import kz.mybrain.superkassa.data.node.NodeVatRate
import kz.mybrain.superkassa.data.node.UnitOfMeasurement

/**
 * Справочники узла, какими их видит рабочее место.
 *
 * Названия видов оплаты, состояний и документов приходят с узла сразу
 * на трёх языках, единицы измерения — из справочника ИС ЭСФ, ставки НДС —
 * с величиной в процентах. Своего перечня ни одного из них в кассе нет:
 * он разошёлся бы с узлом при первой же смене версии протокола или
 * налогового законодательства.
 */
class NodeReference {

    val dictionaries = mutableStateMapOf<Dictionary, List<DictionaryEntry>>()

    /** Единицы измерения ИС ЭСФ: код уходит в чек полем `measureUnitCode`. */
    val units = mutableStateListOf<UnitOfMeasurement>()

    /** Ставки НДС узла вместе с величиной в процентах. */
    val vatRates = mutableStateListOf<NodeVatRate>()

    fun adoptUnits(loaded: List<UnitOfMeasurement>) {
        units.clear()
        units.addAll(loaded)
    }

    fun adoptVatRates(loaded: List<NodeVatRate>) {
        vatRates.clear()
        vatRates.addAll(loaded)
    }

    /** Чего ещё нет: узел мог быть недоступен, когда справочники читались. */
    val missing: Boolean
        get() = units.isEmpty() || vatRates.isEmpty() ||
            Dictionary.entries.any { dictionaries[it].isNullOrEmpty() }
}
