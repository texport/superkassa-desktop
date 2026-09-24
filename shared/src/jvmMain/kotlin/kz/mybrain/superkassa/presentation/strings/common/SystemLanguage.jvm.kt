package kz.mybrain.superkassa.presentation.strings.common

import java.util.Locale

internal actual fun systemLanguage(): String? = Locale.getDefault().language
