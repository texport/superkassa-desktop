package kz.mybrain.superkassa.presentation.strings

import java.util.Locale

internal actual fun systemLanguage(): String? = Locale.getDefault().language
