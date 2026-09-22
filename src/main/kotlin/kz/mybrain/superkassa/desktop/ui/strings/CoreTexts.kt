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
    val shiftClosedShort: String,

    /**
     * Узел состояние смены не назвал.
     *
     * Третье слово здесь не лишнее: «Смена закрыта» на месте неизвестности
     * толкает кассира открыть смену, которую узел, возможно, уже держит
     * открытой, — и он получает отказ вместо начала дня.
     */
    val shiftUnknownShort: String
)

internal val coreTextsRu = CoreTexts(
    details = "Подробности",
    kgdNumber = "Регистрационный номер КГД",
    shiftOpenShort = "Смена открыта",
    shiftClosedShort = "Смена закрыта",
    shiftUnknownShort = "Смена неизвестна"
)

internal val coreTextsKk = CoreTexts(
    details = "Толығырақ",
    kgdNumber = "МКК берген тіркеу нөмірі",
    shiftOpenShort = "Ауысым ашық",
    shiftClosedShort = "Ауысым жабық",
    shiftUnknownShort = "Ауысым белгісіз"
)

internal val coreTextsEn = CoreTexts(
    details = "Details",
    kgdNumber = "Registration number issued by the KGD",
    shiftOpenShort = "Shift is open",
    shiftClosedShort = "Shift is closed",
    shiftUnknownShort = "Shift state unknown"
)

/** Надписи области на выбранном языке. */
fun coreTexts(language: Language): CoreTexts = when (language) {
    Language.Kk -> coreTextsKk
    Language.Ru -> coreTextsRu
    Language.En -> coreTextsEn
}
