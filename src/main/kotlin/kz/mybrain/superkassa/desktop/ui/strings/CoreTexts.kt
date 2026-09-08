package kz.mybrain.superkassa.desktop.ui.strings

/**
 * Надписи области «Главная, каркас и общие части».
 *
 * Файл принадлежит области целиком: надпись заводится один раз
 * и переиспользуется, а правки разных экранов не сходятся в одном файле.
 * Каждое поле обязано существовать во всех трёх языках — об этом
 * заботится компилятор.
 */
data class CoreTexts(
    /** Раскрыть служебные подробности отказа. */
    val details: String,

    /** Регистрационный номер, выданный КГД. */
    val kgdNumber: String,

    /** Смена и её состояние в шапке. */
    val shiftOpenShort: String,
    val shiftClosedShort: String
)

internal val coreTextsRu = CoreTexts(
    details = "Подробности",
    kgdNumber = "Регистрационный номер КГД",
    shiftOpenShort = "Смена открыта",
    shiftClosedShort = "Смена закрыта"
)

internal val coreTextsKk = CoreTexts(
    details = "Толығырақ",
    kgdNumber = "МКК берген тіркеу нөмірі",
    shiftOpenShort = "Ауысым ашық",
    shiftClosedShort = "Ауысым жабық"
)

internal val coreTextsEn = CoreTexts(
    details = "Details",
    kgdNumber = "Registration number issued by the KGD",
    shiftOpenShort = "Shift is open",
    shiftClosedShort = "Shift is closed"
)

/** Надписи области на выбранном языке. */
fun coreTexts(language: Language): CoreTexts = when (language) {
    Language.Kk -> coreTextsKk
    Language.Ru -> coreTextsRu
    Language.En -> coreTextsEn
}
