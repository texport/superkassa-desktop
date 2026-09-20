package kz.mybrain.superkassa.desktop.ui.strings

/**
 * Язык интерфейса.
 *
 * Казахский стоит первым: это государственный язык, и касса работает
 * в Казахстане. Название каждого языка написано на нём самом — так его
 * узнают, не зная двух других.
 */
enum class Language(val code: String, val title: String) {
    Kk("kk", "Қазақша"),
    Ru("ru", "Русский"),
    En("en", "English");

    companion object {
        fun byCode(code: String?): Language = entries.firstOrNull { it.code == code } ?: Kk
    }
}

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
    val history: HistoryStrings,
    val queue: QueueStrings,
    val users: UserStrings,
    val settings: SettingStrings,
    val preview: PreviewStrings,
    val status: StatusStrings,
    val enums: EnumStrings
)

data class CommonStrings(
    val refresh: String,
    val hide: String,
    val print: String,
    val amount: String,
    val pin: String,
    val loading: String,
    val nodeOnline: String,
    val nodeOffline: String,
    val nodeUnavailable: String,
    val refusalCode: String,
    val deliveredToOfd: String,
    val queuedNoLink: String,
    val deliveryState: String,
    val collapse: String,
    val explain: String,
    val expand: String,
    /** Повторить то, что не удалось: одна надпись на все отказы приложения. */
    val retry: String
)

data class LoginStrings(
    val title: String,
    val search: String,
    val noKkms: String,
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

data class ShellStrings(
    val noKkm: String,
    val autonomous: String,
    val blocked: String,
    val changeCashier: String
)

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
