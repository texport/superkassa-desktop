package kz.mybrain.superkassa.strings.api.common

/**
 * Надписи области «Общее»: то, что берут экраны кассира и служебные разделы.
 *
 * Строка объявляется один раз и берётся по смыслу. Разложено по экранам:
 * так видно, что перевод полон, а компилятор не даст забыть язык — новое
 * поле обязано появиться во всех трёх.
 */
data class CommonTexts(
    val general: GeneralTexts,
    val login: LoginTexts,
    val topBar: TopBarTexts,
    val sections: SectionTexts,
    val dashboard: DashboardTexts,
    val autonomous: AutonomousTexts,
    val receipt: ReceiptTexts,
    val returns: ReturnTexts,
    val cash: CashTexts,
    val queue: QueueTexts,
    val users: UserTexts,
    val settingsScreen: SettingsScreenTexts,
    val preview: PreviewTexts,
    val share: ShareTexts,
    val status: StatusTexts,
    val enums: EnumTexts
)

/** Надписи, общие для всех экранов: действия, загрузка, ответ кассы. */
data class GeneralTexts(
    val refresh: String,
    val hide: String,
    val print: String,
    val amount: String,
    val pin: String,
    val loading: String,
    /** Касса поднимается и ещё не готова к работе. */
    val starting: String,
    /** Ответа не дождались, а операция могла состояться. */
    val noAnswer: String,
    /** Касса в процессе не выполнила действие: сбой, а не отказ по существу. */
    val kassaFailed: String,
    val refusalCode: String,
    val deliveredToOfd: String,
    val queuedNoLink: String,
    val deliveryState: String,
    /**
     * Документ снят, а БФД его не принял: `%1$s` — что сделано, `%2$s` —
     * причина словами БФД. Прежде это объявлялось успехом «X-отчёт
     * сформирован. Состояние доставки: отклонён», и причины не было.
     */
    val notAccepted: String,
    val collapse: String,
    val explain: String,
    val expand: String,
    /** Повторить то, что не удалось: одна надпись на все отказы приложения. */
    val retry: String,
    /**
     * В списке выбора нет ни одного значения.
     *
     * Раскрытая пустая рамка читается как сбой приложения, а не как
     * «справочник пуст»: узел мог не отдать его вовсе.
     */
    val nothingToPick: String
)

/** Надписи входа в кассу: список касс, выбор кассы и пин. */
data class LoginTexts(
    val title: String,
    val search: String,
    /**
     * Списка касс нет вовсе.
     *
     * Своё название, а не «Касса не выбрана»: выбирать не из чего, и та
     * надпись называла кассиру другую беду, чем объяснение под ней.
     */
    val noKkmsTitle: String,
    val noKkms: String,

    /**
     * Список касс прочитать не удалось.
     *
     * Отдельно от пустого списка: пустой список — это ответ узла, а
     * молчание и отказ не говорят о кассах на нём ничего. Одна надпись
     * на оба случая утверждала кассиру то, чего приложение не знает.
     */
    val kkmsUnreadTitle: String,
    val kkmsUnread: String,
    val yourKkm: String,
    val pick: String,
    val picked: String,
    val enter: String,
    val reload: String,
    val noKkmChosen: String,
    val pickHint: String,
    val factory: String,
    /** Подпись регистрационного номера КГД в строке списка и рядом с пином. */
    val registrationNumber: String
)

/** Надписи каркаса окна: шапка и её меню. */
data class TopBarTexts(
    val noKkm: String,
    val autonomous: String,
    val blocked: String,
    val changeCashier: String,

    /** Что значат плашки шапки — подсказка по нажатию на плашку. */
    val statusHints: StatusHints,
    /**
     * Меню действий шапки, которым в узком окне не хватило места:
     * обновить, тема, язык, а в самом узком — и смена кассира.
     * Название кассы важнее значков.
     */
    val moreActions: String
)

/**
 * Подсказки к плашкам шапки: что значит состояние и что с ним делать.
 *
 * Кассир видел «В работе» и «Смена открыта» и не понимал, что это за
 * плашки: название без объяснения читалось как украшение.
 */
data class StatusHints(
    val active: String,
    val blocked: String,
    val programming: String,
    val registration: String,
    val autonomous: String,
    val shiftOpen: String,
    val shiftClosed: String
)

/** Названия разделов навигации. */
data class SectionTexts(
    val dashboard: String,
    val sale: String,
    val returns: String,
    val cash: String,
    val history: String,
    val queue: String,
    val users: String,
    val settings: String,
    val register: String,
    val cabinet: String,
    /** Кассы рабочего места и вход по пину — первый раздел окна до входа. */
    val kkms: String,
    /** Кнопка шапки, которая на телефоне открывает все разделы разом. */
    val menu: String
)
