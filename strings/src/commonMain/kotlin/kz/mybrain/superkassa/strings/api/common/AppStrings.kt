package kz.mybrain.superkassa.strings.api.common

/**
 * Все надписи кассы в одном месте.
 *
 * Строка объявляется один раз и берётся по смыслу. Разложено по экранам:
 * так видно, что перевод полон, а компилятор не даст забыть язык — новое
 * поле обязано появиться во всех трёх.
 */
data class AppStrings(
    val common: CommonStrings,
    val login: LoginStrings,
    val shell: ShellStrings,
    val sections: SectionStrings,
    val dashboard: DashboardStrings,
    val autonomous: AutonomousStrings,
    val sale: SaleStrings,
    val returns: ReturnStrings,
    val cash: CashStrings,
    val queue: QueueStrings,
    val users: UserStrings,
    val settings: SettingStrings,
    val preview: PreviewStrings,
    val status: StatusStrings,
    val enums: EnumStrings
)

/** Надписи, общие для всех экранов: действия, загрузка, ответ кассы. */
data class CommonStrings(
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
data class LoginStrings(
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
data class ShellStrings(
    val noKkm: String,
    val autonomous: String,
    val blocked: String,
    val changeCashier: String,
    /**
     * Меню действий шапки, которым в узком окне не хватило места:
     * обновить, тема, язык, а в самом узком — и смена кассира.
     * Название кассы важнее значков.
     */
    val moreActions: String
)

/** Названия разделов навигации. */
data class SectionStrings(
    val dashboard: String,
    val sale: String,
    val returns: String,
    val cash: String,
    val history: String,
    val queue: String,
    val users: String,
    val settings: String,
    val register: String,
    val cabinet: String
)
