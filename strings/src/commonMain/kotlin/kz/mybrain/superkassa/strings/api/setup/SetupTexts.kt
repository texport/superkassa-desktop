package kz.mybrain.superkassa.strings.api.setup

/**
 * Надписи области «Подключение кассы».
 *
 * Мастер показывает по одному шагу на экран, и у каждого шага три надписи:
 * название того, что происходит, объяснение — что делает шаг и зачем он, —
 * и действие. Владелец, вернувшийся к брошенному мастеру через неделю,
 * должен понять, где он остановился и что делать дальше, не вспоминая,
 * с чего начинал.
 */
data class SetupTexts(
    /** «Шаг 2 из 5»: `%1$s` — номер шага, `%2$s` — сколько их всего. */
    val stepOf: String,
    val next: String,
    val back: String,
    val startOver: String,

    /**
     * Вопрос перед тем, как забыть пройденное.
     *
     * Пройденное живёт на диске, и «Начать заново» стирает его одним
     * нажатием: заводской номер, уже унесённый в кабинет, и кассу,
     * заведённую там под ним. Сами они из кабинета не исчезают — мастер
     * про них забывает, и владелец заводит вторую кассу под вторым
     * номером. Об этом и сказано в вопросе.
     */
    val startOverAsk: String,
    val startOverExplain: String,
    val stepWay: String,
    val wayExplain: String,
    val viaCabinet: String,
    val viaCabinetHint: String,
    val manually: String,
    val manuallyHint: String,
    val stepFactory: String,
    val factoryExplain: String,

    /** Заводской номер на ручном пути: он нужен тому, кто ещё будет заводить кассу в БФД. */
    val factoryExplainManual: String,
    val getFactory: String,

    /** Год выпуска под заводским номером: `%s` — год. */
    val factoryYear: String,
    val stepCabinet: String,
    val cabinetExplain: String,
    val signInFirst: String,
    val addedToCabinet: String,
    val stepApplication: String,
    val applicationExplain: String,
    val submit: String,
    val registered: String,
    val status: String,
    val stepCredentials: String,
    val credentialsExplain: String,
    val stepAdmin: String,
    val adminExplain: String,

    /** Пин на ручном пути: токен владелец набрал сам, и о выпуске токена ни слова. */
    val adminExplainManual: String,
    val connect: String,
    val connected: String,
    val done: String,

    /**
     * Касса ответила, что завела кассу, а сохранить её не смогла.
     *
     * Так бывает, когда БФД не ответила на заведение: касса процесса
     * возвращает сведения о кассе, которой в её базе нет. Сказать
     * «касса подключена» над такой кассой значит отправить владельца
     * входить в то, чего нет.
     */
    val notStored: String,

    /**
     * Кабинет токена не выдал: касса в КГД ещё не поставлена на учёт.
     *
     * Прежде нажатие «Подключить» в этом случае проходило молча.
     */
    val noToken: String,

    /**
     * Пин администратора ещё раз.
     *
     * Стандартного пина у новой кассы нет: войти в неё можно только пином,
     * заданным здесь, и опечатка в нём закрывала бы кассу от владельца.
     */
    val pinRepeat: String,
    val pinsDiffer: String
)
