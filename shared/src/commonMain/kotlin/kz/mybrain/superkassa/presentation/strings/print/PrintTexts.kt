package kz.mybrain.superkassa.presentation.strings.print

import kz.mybrain.superkassa.presentation.strings.common.Language

/**
 * Надписи печати, которых нет в общем наборе просмотра.
 *
 * Отказы устройства, на котором печатать и сохранять пока некуда: молчание
 * вместо них выглядело бы так, будто форма сохранена.
 */
data class PrintTexts(
    /** Сохранять форму в файл на этом устройстве некуда. */
    val keepUnavailable: String,
    /** Принтера, выбранного для кассы, в системе больше нет. */
    val printerGone: String
)

/** Надписи печати на выбранном языке. */
fun printTexts(language: Language): PrintTexts = when (language) {
    Language.Kk -> PrintTexts(
        keepUnavailable = "Бұл құрылғыда пішінді файлға сақтау әзірге мүмкін емес: оны басқа машинада ашыңыз",
        printerGone = "Кассаның принтері жүйеде табылмады: ол ажыратылған немесе жойылған. Қосыңыз " +
            "немесе баптауларда басқа принтерді таңдаңыз"
    )
    Language.Ru -> PrintTexts(
        keepUnavailable = "На этом устройстве сохранить форму в файл пока нельзя: откройте её на другой машине",
        printerGone = "Принтер кассы не найден в системе: он отключён или удалён. Подключите его " +
            "или выберите другой принтер в настройках"
    )
    Language.En -> PrintTexts(
        keepUnavailable = "This device cannot save the form to a file yet: open it on another machine",
        printerGone = "The register's printer is not found in the system: it is disconnected or removed. " +
            "Connect it or choose another printer in the settings"
    )
}
