package kz.mybrain.superkassa.desktop.ui.strings

/**
 * Надписи ожидания подписи ЭЦП.
 *
 * Отдельный набор, а не строки в [CabinetTexts]: набор кабинета уже стоял
 * у предела JVM в 255 аргументов конструктора, и новое поле в нём —
 * отложенный `ClassFormatError` при загрузке класса, которого не поймает
 * ни сборка, ни тест. Речь тут и не о кабинете: подпись просят и заявления
 * в КГД, а ждут её всегда одинаково.
 *
 * Сказано в оставшемся времени, а не в «подождите»: владелец должен
 * понимать, ждать ему ещё или чинить.
 */
data class EdsTexts(
    /** Сколько ожидания осталось: подставляется «м:сс». */
    val remaining: String,

    /** Прервать ожидание — тем же местом, где оно началось. */
    val cancelWait: String
)

/** Надписи на языке владельца. */
fun edsTexts(language: Language): EdsTexts = when (language) {
    Language.Kk -> edsTextsKk
    Language.Ru -> edsTextsRu
    Language.En -> edsTextsEn
}

private val edsTextsRu = EdsTexts(
    remaining = "Осталось %1\$s",
    cancelWait = "Отменить ожидание"
)

private val edsTextsKk = EdsTexts(
    remaining = "%1\$s қалды",
    cancelWait = "Күтуді тоқтату"
)

private val edsTextsEn = EdsTexts(
    remaining = "%1\$s left",
    cancelWait = "Stop waiting"
)
