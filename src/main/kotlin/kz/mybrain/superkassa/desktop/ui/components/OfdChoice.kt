package kz.mybrain.superkassa.desktop.ui.components

import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.desktop.server.DictionaryEntry

/**
 * Выбор ОФД и контура.
 *
 * Один и тот же набор полей в мастере подключения и в кабинете: где бы
 * владелец ни заводил кассу, куда она шлёт чеки — вопрос один и тот же.
 */
@Composable
fun OfdChoice(
    target: OfdTarget,
    providers: List<DictionaryEntry>,
    environments: List<DictionaryEntry>,
    language: String,
    onChange: (OfdTarget) -> Unit
) {
    ProviderPicker(providers, language, target.provider) { onChange(target.copy(provider = it)) }
    EnvironmentPicker(environments, language, target.environment) { onChange(target.copy(environment = it)) }
}
