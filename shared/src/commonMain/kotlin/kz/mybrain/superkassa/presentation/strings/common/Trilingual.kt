package kz.mybrain.superkassa.presentation.strings.common

import io.github.texport.superkassa.core.presentation.api.model.reference.TrilingualMessageResponse

/**
 * Слова кассы на языке кассира — одно правило на все экраны.
 *
 * Справочники ядра и отказы кассы говорят сразу на трёх языках, и выбирать
 * нужный обязан один код, а не каждый экран по-своему. Пустое на казахском
 * или английском подменяется русским: чужой язык лучше пустой строки.
 * Пустое и по-русски — не слова: `null`, и экран берёт своё.
 */
fun Language.choose(ru: String, kk: String, en: String): String? = when (this) {
    Language.Ru -> ru
    Language.Kk -> kk.ifBlank { ru }
    Language.En -> en.ifBlank { ru }
}.takeIf { it.isNotBlank() }

/** Название из справочника кассы на языке кассира; `null` — справочник не назвал. */
fun TrilingualMessageResponse.of(language: Language): String? = language.choose(ru, kk, en)
