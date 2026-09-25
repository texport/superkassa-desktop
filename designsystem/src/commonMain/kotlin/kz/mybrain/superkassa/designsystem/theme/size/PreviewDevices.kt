package kz.mybrain.superkassa.designsystem.theme.size

/**
 * Окна, в которых экраны смотрят в превью Android Studio, — точки окна.
 *
 * По одному окну на класс ширины Material 3 и на то, чем кассу держат
 * в руках: телефон, складной, планшет стоймя и лёжа, ноутбук и настольный
 * монитор, широкий монитор. Раскладка решается классом окна, и превью
 * показывает каждый класс, а не одну удачную ширину.
 *
 * Числа записаны постоянными, а не токенами в точках: аннотация превью
 * принимает только постоянные. Их же проходит проверка превью, так что
 * в Android Studio и в проверке экран рисуется на одних и тех же окнах.
 */
object PreviewDevices {
    /** Телефон — компактное окно. */
    const val PHONE_WIDTH = 411
    const val PHONE_HEIGHT = 891

    /** Складной телефон раскрытым — среднее окно. */
    const val FOLDABLE_WIDTH = 673
    const val FOLDABLE_HEIGHT = 841

    /** Планшет стоймя — среднее окно. */
    const val TABLET_WIDTH = 800
    const val TABLET_HEIGHT = 1280

    /** Планшет лёжа и ноутбук — расширенное окно. */
    const val TABLET_WIDE_WIDTH = 1280
    const val TABLET_WIDE_HEIGHT = 800

    /** Настольный монитор — большое окно. */
    const val DESKTOP_WIDTH = 1920
    const val DESKTOP_HEIGHT = 1080

    /** Широкий монитор — очень большое окно. */
    const val WIDE_WIDTH = 2560
    const val WIDE_HEIGHT = 1440

    /** Все окна экрана по порядку ширины: ширина и высота в точках. */
    val screens: List<Pair<Int, Int>> = listOf(
        PHONE_WIDTH to PHONE_HEIGHT,
        FOLDABLE_WIDTH to FOLDABLE_HEIGHT,
        TABLET_WIDTH to TABLET_HEIGHT,
        TABLET_WIDE_WIDTH to TABLET_WIDE_HEIGHT,
        DESKTOP_WIDTH to DESKTOP_HEIGHT,
        WIDE_WIDTH to WIDE_HEIGHT
    )
}
