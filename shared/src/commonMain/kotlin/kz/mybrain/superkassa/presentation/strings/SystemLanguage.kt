package kz.mybrain.superkassa.presentation.strings

/**
 * Код языка системы — `ru`, `kk`, `en`, — или `null`, если система
 * его не называет.
 *
 * Спрашивается у каждой платформы своим способом: общего способа узнать
 * язык машины у Kotlin нет.
 */
internal expect fun systemLanguage(): String?
