package kz.mybrain.superkassa.presentation.words.common

import io.github.texport.superkassa.core.presentation.api.model.reference.TrilingualMessageResponse
import kz.mybrain.superkassa.strings.api.Language

/**
 * Название из справочника кассы на языке кассира; `null` — справочник не назвал.
 *
 * Правило выбора языка одно на все экраны — [Language.choose]; здесь только
 * перевод справочного типа ядра в три строки.
 */
fun TrilingualMessageResponse.of(language: Language): String? = language.choose(ru, kk, en)
