package kz.mybrain.superkassa.presentation.common.message

import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.strings.api.Language

/** Слова отказа на языке кассира. */
fun Answer.Refused.words(language: Language): String = language.choose(ru, kk, en).orEmpty()
