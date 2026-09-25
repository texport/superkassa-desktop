package kz.mybrain.superkassa.strings.api.cabinet.register

/** Регистрационная карта кассы и её прежние версии. */
data class RegistrationCardTexts(
    val title: String,
    val missing: String,
    val savePdf: String,

    /** Версии регистрационной карты: прежние записи КГД о кассе. */
    val versions: String,
    val versionsEmpty: String,
    val version: String,
    val currentVersion: String,
    val changed: String
)
