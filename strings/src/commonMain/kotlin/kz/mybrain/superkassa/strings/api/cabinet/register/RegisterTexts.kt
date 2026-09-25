package kz.mybrain.superkassa.strings.api.cabinet.register

/** Паспорт кассы: её записи в кабинете, техническое состояние у БФД и токен. */
data class RegisterTexts(
    val passport: String,
    val internalName: String,
    val factoryNumber: String,
    val factoryLocked: String,
    val manufactureYear: String,
    val model: String,
    val registrationNumber: String,
    val status: String,
    val technicalState: String,
    val technicalUnknown: String,
    val trafficSuspended: String,
    val bfdDisconnected: String,
    val shift: String,
    val lastContact: String,
    val lastContactNever: String,

    /** Снимок БФД: номер смены, связь и отсутствие того и другого. */
    val shiftNumberTitle: String,
    val shiftNumberNone: String,
    val token: String,
    val issueToken: String,
    val tokenIssued: String,
    val tokenGoesToNode: String,
    val tokenUnconfirmed: String,
    val tokenNeedsNode: String,
    val tokenOnlyRegistered: String,
    val localInfoSynced: String,
    val localInfoNeedsSync: String,
    val delete: String,
    val deleteOnlyDraft: String,
    val actionsJournal: String,
    val actionsEmpty: String,

    /** Сверка состояния кассы по трём источникам. */
    val state: RegisterStateTexts,

    /** Регистрационная карта и её версии. */
    val card: RegistrationCardTexts
)
