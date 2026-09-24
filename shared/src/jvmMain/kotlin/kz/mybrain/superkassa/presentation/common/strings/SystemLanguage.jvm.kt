package kz.mybrain.superkassa.presentation.common.strings

import java.util.Locale

internal actual fun systemLanguage(): String? = Locale.getDefault().language
