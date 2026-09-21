package kz.mybrain.superkassa.desktop.ui.components

import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.desktop.server.DictionaryEntry

/**
 * Выбор контура БФД.
 *
 * Одно и то же поле в мастере подключения и в кабинете: где бы владелец
 * ни заводил кассу, куда она шлёт чеки — вопрос один и тот же.
 *
 * Поставщика здесь нет намеренно. Он один на весь продукт и подставлен
 * в [OfdTarget]; поле выбора с единственной строкой требовало от владельца
 * действия, у которого нет второго исхода.
 */
@Composable
fun OfdChoice(
    target: OfdTarget,
    environments: List<DictionaryEntry>,
    language: String,
    onChange: (OfdTarget) -> Unit
) {
    EnvironmentPicker(environments, language, target.environment) { onChange(target.copy(environment = it)) }
}
