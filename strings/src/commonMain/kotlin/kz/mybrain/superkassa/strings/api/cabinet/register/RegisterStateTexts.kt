package kz.mybrain.superkassa.strings.api.cabinet.register

/** Сверка состояния кассы: что о ней говорят эта машина, кабинет и БФД. */
data class RegisterStateTexts(
    /** Сверка состояний: кто о кассе говорит и что именно. */
    val disagree: String,
    val disagreeNote: String,
    val sourceNode: String,
    val sourceCabinet: String,
    val sourceBfd: String,

    /** Ответ по существу: то, что владелец читает первой строкой. */
    val working: String,
    val blocked: String,
    val offRecord: String,
    val workUnknown: String,
    val shiftUnknown: String,

    /**
     * Показание одного источника.
     *
     * Плашка читается отдельно от всего: не «Нет», а «касса снята
     * с учёта» — под общим «Нет» владелец не понимал ни того, о чём речь,
     * ни того, что ему делать.
     */
    val claimWorking: String,
    val claimBlocked: String,
    val claimOnRecord: String,
    val claimOffRecord: String,
    val claimRecordUnread: String,

    /**
     * Что кабинет говорит о кассе, которой на учёте нет.
     *
     * Причин этому три, и владельцу они говорят разное: на учёт ещё
     * не подавали, заявление рассматривают, в учёте отказано. Под общим
     * «касса снята с учёта» они не различались вовсе.
     */
    val claimNotFiled: String,
    val claimIsnaPending: String,
    val claimIsnaRefused: String,
    val claimNodeNoKkm: String,
    val claimBfdNoKkm: String,
    val claimBfdNoAnswer: String,
    val claimShiftOpen: String,
    val claimShiftClosed: String,
    val claimShiftNotKept: String
)
