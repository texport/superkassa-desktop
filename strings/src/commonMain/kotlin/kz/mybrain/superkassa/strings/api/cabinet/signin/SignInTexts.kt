package kz.mybrain.superkassa.strings.api.cabinet.signin

/** Вход в кабинет по ЭЦП: кнопки двери и то, чем вошедший назван. */
data class SignInTexts(
    val signIn: String,
    val signing: String,
    val signOut: String,
    val address: String,
    val bin: String,
    val iin: String
)
