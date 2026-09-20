package kz.mybrain.superkassa.desktop.ui.strings

/**
 * Надписи области «Подключение кассы».
 *
 * Шаги названы тем, что происходит, а не порядковым номером: владелец,
 * вернувшийся к брошенному мастеру через неделю, должен понять, где он
 * остановился, не считая шаги заново.
 */
data class SetupTexts(
    val title: String,
    val explain: String,
    val startOver: String,
    val stepFactory: String,
    val stepFactoryHint: String,
    val getFactory: String,
    val stepCabinet: String,
    val stepCabinetHint: String,
    val signInFirst: String,
    val addToCabinet: String,
    val addedToCabinet: String,
    val stepApplication: String,
    val stepApplicationHint: String,
    val submit: String,
    val awaitingIsna: String,
    val registered: String,
    val stepAdmin: String,
    val stepAdminHint: String,
    val connect: String,
    val connected: String,
    val done: String,
    val waiting: String,
    val viaCabinet: String,
    val manually: String,
    val manuallyHint: String,
    val status: String
)

private val setupTextsRu = SetupTexts(
    title = "Подключение кассы",
    explain = "Касса заводится в ОФД и в узле подряд: номер отсюда уходит в кабинет, " +
        "а идентификатор и токен возвращаются обратно. Мастер можно закрыть и продолжить позже.",
    startOver = "Начать заново",
    stepFactory = "Заводской номер",
    stepFactoryHint = "Номер выдаёт узел и запоминает: второй запрос дал бы другой",
    getFactory = "Получить номер",
    stepCabinet = "Касса в кабинете ОФД",
    stepCabinetHint = "Вход по ЭЦП владельца, затем касса заводится с этим заводским номером",
    signInFirst = "Сначала войдите в кабинет по ЭЦП",
    addToCabinet = "Завести кассу в кабинете",
    addedToCabinet = "Касса заведена в кабинете",
    stepApplication = "Постановка на учёт",
    stepApplicationHint = "Заявление подписывается ЭЦП и уходит в КГД; ответ приходит не сразу",
    submit = "Подать заявление",
    awaitingIsna = "Заявление отправлено, ждём ответа КГД",
    registered = "Касса на учёте",
    stepAdmin = "Касса в узле и пин администратора",
    stepAdminHint = "Токен выдаётся кабинетом и в файл не пишется: продолжите позже — выдадим новый",
    connect = "Завести кассу",
    connected = "Касса подключена — можно входить",
    done = "готово",
    waiting = "ждёт предыдущего шага",
    viaCabinet = "Через кабинет",
    manually = "Вручную",
    manuallyHint = "Когда кассу в ОФД завели без вас: идентификатор и токен уже на руках",
    status = "Состояние кассы:"
)

private val setupTextsKk = SetupTexts(
    title = "Кассаны қосу",
    explain = "Касса ОФД мен түйінде кезекпен тіркеледі: нөмір осы жерден кабинетке кетеді, " +
        "идентификатор мен токен кері оралады. Шеберді жауып, кейін жалғастыруға болады.",
    startOver = "Қайтадан бастау",
    stepFactory = "Зауыттық нөмір",
    stepFactoryHint = "Нөмірді түйін береді және есте сақтайды: екінші сұрау басқасын берер еді",
    getFactory = "Нөмір алу",
    stepCabinet = "ОФД кабинетіндегі касса",
    stepCabinetHint = "Иесінің ЭЦҚ-мен кіру, содан кейін касса осы зауыттық нөмірмен тіркеледі",
    signInFirst = "Алдымен кабинетке ЭЦҚ арқылы кіріңіз",
    addToCabinet = "Кабинетте касса қосу",
    addedToCabinet = "Касса кабинетте тіркелді",
    stepApplication = "Есепке қою",
    stepApplicationHint = "Өтініш ЭЦҚ-мен қол қойылып МКК-ға жіберіледі; жауап бірден келмейді",
    submit = "Өтініш беру",
    awaitingIsna = "Өтініш жіберілді, МКК жауабын күтудеміз",
    registered = "Касса есепте",
    stepAdmin = "Түйіндегі касса және әкімші пині",
    stepAdminHint = "Токенді кабинет береді және файлға жазылмайды: кейін жалғастырсаңыз, жаңасы беріледі",
    connect = "Кассаны тіркеу",
    connected = "Касса қосылды — кіруге болады",
    done = "дайын",
    waiting = "алдыңғы қадамды күтуде",
    viaCabinet = "Кабинет арқылы",
    manually = "Қолмен",
    manuallyHint = "Кассаны ОФД-да сізсіз тіркеген жағдайда: идентификатор мен токен қолда бар",
    status = "Касса күйі:"
)

private val setupTextsEn = SetupTexts(
    title = "Connecting a cash register",
    explain = "The register is created in the OFD and in the node one after another: the factory number " +
        "goes from here to the cabinet, and the identifier and token come back. The wizard can be closed and resumed.",
    startOver = "Start over",
    stepFactory = "Factory number",
    stepFactoryHint = "The node issues the number and it is remembered: a second request would give another",
    getFactory = "Get the number",
    stepCabinet = "The register in the OFD cabinet",
    stepCabinetHint = "Sign in with the owner's certificate, then create the register with this factory number",
    signInFirst = "Sign in to the cabinet first",
    addToCabinet = "Create in the cabinet",
    addedToCabinet = "Created in the cabinet",
    stepApplication = "Registration",
    stepApplicationHint = "The application is signed and sent to the KGD; the answer does not come at once",
    submit = "Submit the application",
    awaitingIsna = "Application sent, waiting for the KGD",
    registered = "The register is on record",
    stepAdmin = "The register in the node and the admin PIN",
    stepAdminHint = "The token is issued by the cabinet and never written to a file: resume later and a new one is issued",
    connect = "Create the register",
    connected = "The register is connected — you can sign in",
    done = "done",
    waiting = "waiting for the previous step",
    viaCabinet = "Through the cabinet",
    manually = "By hand",
    manuallyHint = "When someone else created the register in the OFD: you already have the identifier and the token",
    status = "Register status:"
)

/** Надписи области на выбранном языке. */
fun setupTexts(language: Language): SetupTexts = when (language) {
    Language.Kk -> setupTextsKk
    Language.Ru -> setupTextsRu
    Language.En -> setupTextsEn
}
