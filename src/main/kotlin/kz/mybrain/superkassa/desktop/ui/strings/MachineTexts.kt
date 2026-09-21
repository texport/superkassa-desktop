package kz.mybrain.superkassa.desktop.ui.strings

/**
 * Надписи о работе кассы на этой машине.
 *
 * Отдельный набор, а не строки в [CabinetTexts]: речь не о том, что о кассе
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
    val retry: String
)

/** Надписи на языке владельца. */
fun machineTexts(language: Language): MachineTexts = when (language) {
    Language.Kk -> machineTextsKk
    Language.Ru -> machineTextsRu
    Language.En -> machineTextsEn
}

private val machineTextsRu = MachineTexts(
    title = "Работа на этой машине",
    worksHere = "Касса заведена на этой машине",
    goToKkm = "Перейти к кассе",
    onlyOnRecord = "Работать можно только с кассы, стоящей на учёте в КГД",
    notHere = "На этой машине касса не заведена",
    workHere = "Работать на этой машине",
    tokenReissued = "Токен будет выпущен заново: действующий кабинет показать не умеет. " +
        "Прежний токен перестанет действовать.",
    heardByOfd = "БФД принимала данные от этой кассы",
    handoverUnderstood = "Понимаю: на другой машине эта касса работать перестанет",
    done = "Касса заведена — кассир входит в неё по этому пину администратора",
    stranded = "Токен выпущен, а касса на этой машине не заведена. Повторите: уйдёт тот же " +
        "токен, второй раз он не выпускается.",
    retry = "Повторить"
)

private val machineTextsKk = MachineTexts(
    title = "Осы машинадағы жұмыс",
    worksHere = "Касса осы машинада тіркелген",
    goToKkm = "Кассаға өту",
    onlyOnRecord = "МКК есебінде тұрған кассамен ғана жұмыс істеуге болады",
    notHere = "Осы машинада касса тіркелмеген",
    workHere = "Осы машинада жұмыс істеу",
    tokenReissued = "Токен қайта шығарылады: қолданыстағысын кабинет көрсете алмайды. " +
        "Бұрынғы токен күшін жояды.",
    heardByOfd = "БФД осы кассадан дерек қабылдаған",
    handoverUnderstood = "Түсінемін: басқа машинада бұл касса жұмыс істемей қалады",
    done = "Касса тіркелді — кассир осы әкімші пінімен кіреді",
    stranded = "Токен шығарылды, ал касса осы машинада тіркелмеді. Қайталаңыз: сол токен " +
        "қайта жіберіледі, екінші рет шығарылмайды.",
    retry = "Қайталау"
)

private val machineTextsEn = MachineTexts(
    title = "Work on this machine",
    worksHere = "The register is set up on this machine",
    goToKkm = "Go to the register",
    onlyOnRecord = "Only a register on record with the KGD can be worked from",
    notHere = "The register is not set up on this machine",
    workHere = "Work on this machine",
    tokenReissued = "The token will be issued anew: the cabinet cannot show the current one. " +
        "The previous token stops working.",
    heardByOfd = "The BFD has received data from this register",
    handoverUnderstood = "I understand: on the other machine this register will stop working",
    done = "The register is set up — the cashier signs in with this administrator PIN",
    stranded = "The token was issued, but the register was not set up on this machine. Retry: " +
        "the same token is sent again, it is not issued twice.",
    retry = "Retry"
)
