package kz.mybrain.superkassa.strings.api.cabinet.applications

/** Заявления в КГД: постановка, перерегистрация и снятие с учёта, ход подачи. */
data class ApplicationTexts(
    val title: String,
    val registration: String,
    val reregistration: String,
    val deregistration: String,
    val reason: String,
    val reasonCessation: String,
    val reasonBroken: String,
    val reasonLost: String,
    val reasonOther: String,
    val comment: String,
    val newPlace: String,
    val registerStatus: String,
    val submit: String,
    val sent: String,
    val wait: String,
    val failed: String,
    val inFlight: String,
    val none: String,
    val stagePreparing: String,
    val stageSigning: String,
    val stageSending: String,

    /** Кабинет отказал снять кассу: смена не закрыта. */
    val shiftOpenTitle: String,
    val shiftOpenAsk: String,
    val shiftOpenElsewhere: String,
    val closeShiftAndDeregister: String,
    val adminPin: String
)
