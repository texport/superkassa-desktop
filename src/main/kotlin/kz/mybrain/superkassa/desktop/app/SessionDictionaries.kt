package kz.mybrain.superkassa.desktop.app

import kz.mybrain.superkassa.desktop.server.Dictionary
import kz.mybrain.superkassa.desktop.server.dictionary
import kz.mybrain.superkassa.desktop.server.unitsOfMeasurement
import kz.mybrain.superkassa.desktop.server.vatRates
import kz.mybrain.superkassa.desktop.ui.strings.documentFallback
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs

/**
 * Справочники узла: виды оплаты, состояния доставки, виды документов.
 *
 * Названия приходят с узла сразу на трёх языках: свой перевод
 * в приложении рано или поздно разошёлся бы с тем, что напечатано на чеке.
 */
suspend fun Session.loadDictionaries() {
    Dictionary.entries.forEach { read(it) }
    readUnits()
    readVatRates()
}

/**
 * Дочитывает справочники, которые в прошлый раз не пришли.
 *
 * Читаются они один раз при входе, и узел в этот миг мог быть недоступен:
 * тогда список видов оплаты или перечень ОФД оставался пустым до конца
 * смены, а форма заведения кассы — незаполнимой. Перечитывается только
 * пустое: пришедшее менять незачем.
 */
suspend fun Session.loadMissingDictionaries() {
    if (!referenceMissing) return
    Dictionary.entries.filter { dictionaries[it].isNullOrEmpty() }.forEach { read(it) }
    if (units.isEmpty()) readUnits()
    if (vatRates.isEmpty()) readVatRates()
}

/**
 * Название значения справочника на языке кассира.
 *
 * Если узел такого кода не знает — а в журнале остались записи прежних
 * версий, — берётся своё название. Голый код на экране кассы недопустим.
 */
fun Session.titleOf(dictionary: Dictionary, code: String?): String {
    if (code == null) return Glyphs.DASH
    val fromNode = dictionaries[dictionary]?.firstOrNull { it.code == code }?.title(language.code)
    if (fromNode != null && fromNode != code) return fromNode
    val own = if (dictionary == Dictionary.DocumentTypes) texts.enums.documentFallback(code) else null
    return own ?: code
}

/** Единицы измерения ИС ЭСФ: без них позиция уходит в ОФД штукой. */
private suspend fun Session.readUnits() {
    guard<Unit>(texts.settings.title) { adoptUnits(client.unitsOfMeasurement()) }
}

/** Ставки НДС с величиной: кассир обязан видеть, по какой пробивает. */
private suspend fun Session.readVatRates() {
    guard<Unit>(texts.settings.title) { adoptVatRates(client.vatRates()) }
}

/** Читает один справочник; недоступность узла названа настройками. */
private suspend fun Session.read(dictionary: Dictionary) {
    guard<Unit>(texts.settings.title) {
        dictionaries[dictionary] = client.dictionary(dictionary)
    }
}
