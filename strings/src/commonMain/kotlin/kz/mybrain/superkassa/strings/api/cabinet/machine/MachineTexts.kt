package kz.mybrain.superkassa.strings.api.cabinet.machine

/**
 * Надписи о работе кассы на этой машине.
 *
 * Свой набор, а не строки паспорта кассы: речь не о том, что о кассе
 * записано в кабинете, а о том, встанет ли за неё кассир здесь. Слова
 * говорят о последствиях действия — перевыпущенный токен останавливает
 * кассу на соседней машине, — и сказаны они прямо, а не намёком.
 */
data class MachineTexts(
    val title: String,
    val worksHere: String,
    val goToKkm: String,
    val onlyOnRecord: String,
    val notHere: String,
    val workHere: String,
    val tokenReissued: String,
    val heardByOfd: String,
    val handoverUnderstood: String,
    val done: String,
    val stranded: String,
    /** Касса не заводится без ОКЭДа: что сделать владельцу. */
    val okedMissing: String,
    val retry: String
)
