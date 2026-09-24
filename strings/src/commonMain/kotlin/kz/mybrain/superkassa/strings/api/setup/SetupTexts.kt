package kz.mybrain.superkassa.strings.api.setup

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
    val stepFactory: String,
    val stepFactoryHint: String,
    val getFactory: String,
    val stepCabinet: String,
    val stepCabinetHint: String,
    val signInFirst: String,
    val addedToCabinet: String,
    val stepApplication: String,
    val stepApplicationHint: String,
    val submit: String,
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
    val status: String,

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
     * Кабинет токена не выдал: касса в ИСНА ещё не поставлена на учёт.
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
