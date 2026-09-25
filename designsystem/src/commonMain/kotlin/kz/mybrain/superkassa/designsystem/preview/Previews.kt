package kz.mybrain.superkassa.designsystem.preview

import androidx.compose.ui.tooling.preview.AndroidUiModes
import androidx.compose.ui.tooling.preview.Preview
import kz.mybrain.superkassa.designsystem.theme.size.PreviewDevices

/**
 * Превью экрана: все классы окна Material 3 и тёмная тема.
 *
 * Экран смотрят сразу на телефоне, складном, планшете стоймя и лёжа,
 * настольном и широком мониторе: раскладка меняется классом окна, и одно
 * удачное окно не говорит ничего о соседних. Тёмная тема — на планшете
 * лёжа: там видно больше всего поверхностей сразу.
 *
 * Аннотация одна на всё приложение: окна берутся из [PreviewDevices],
 * а не набираются у каждого экрана. Её же находит проверка превью —
 * аннотация видна во время исполнения, в отличие от `@Preview`.
 */
@Target(AnnotationTarget.FUNCTION)
@Preview(
    name = "Телефон",
    group = SCREEN,
    widthDp = PreviewDevices.PHONE_WIDTH,
    heightDp = PreviewDevices.PHONE_HEIGHT,
    showBackground = true
)
@Preview(
    name = "Складной",
    group = SCREEN,
    widthDp = PreviewDevices.FOLDABLE_WIDTH,
    heightDp = PreviewDevices.FOLDABLE_HEIGHT,
    showBackground = true
)
@Preview(
    name = "Планшет стоймя",
    group = SCREEN,
    widthDp = PreviewDevices.TABLET_WIDTH,
    heightDp = PreviewDevices.TABLET_HEIGHT,
    showBackground = true
)
@Preview(
    name = "Планшет лёжа",
    group = SCREEN,
    widthDp = PreviewDevices.TABLET_WIDE_WIDTH,
    heightDp = PreviewDevices.TABLET_WIDE_HEIGHT,
    showBackground = true
)
@Preview(
    name = "Настольный",
    group = SCREEN,
    widthDp = PreviewDevices.DESKTOP_WIDTH,
    heightDp = PreviewDevices.DESKTOP_HEIGHT,
    showBackground = true
)
@Preview(
    name = "Широкий монитор",
    group = SCREEN,
    widthDp = PreviewDevices.WIDE_WIDTH,
    heightDp = PreviewDevices.WIDE_HEIGHT,
    showBackground = true
)
@Preview(
    name = "Планшет лёжа, тёмная",
    group = SCREEN,
    widthDp = PreviewDevices.TABLET_WIDE_WIDTH,
    heightDp = PreviewDevices.TABLET_WIDE_HEIGHT,
    showBackground = true,
    uiMode = NIGHT
)
annotation class ScreenPreviews

/**
 * Превью элемента: светлая и тёмная тема на ширине телефона.
 *
 * Элемент — строка, группа, поле — смотрят в самом узком окне: там ему
 * теснее всего, а шире он только растягивается по раскладке.
 */
@Target(AnnotationTarget.FUNCTION)
@Preview(name = "Светлая", group = ELEMENT, widthDp = PreviewDevices.PHONE_WIDTH, showBackground = true)
@Preview(
    name = "Тёмная",
    group = ELEMENT,
    widthDp = PreviewDevices.PHONE_WIDTH,
    showBackground = true,
    uiMode = NIGHT
)
annotation class ElementPreviews

/**
 * Превью панели: телефон целиком, светлая и тёмная тема.
 *
 * Панель — список разделов или открытый раздел — на телефоне занимает весь
 * экран, а шире встаёт рядом с соседней той же колонкой. Смотреть её
 * отдельно от экрана удобнее всего в окне телефона: высота у неё своя,
 * и прокрутка видна.
 */
@Target(AnnotationTarget.FUNCTION)
@Preview(
    name = "Светлая",
    group = PANE,
    widthDp = PreviewDevices.PHONE_WIDTH,
    heightDp = PreviewDevices.PHONE_HEIGHT,
    showBackground = true
)
@Preview(
    name = "Тёмная",
    group = PANE,
    widthDp = PreviewDevices.PHONE_WIDTH,
    heightDp = PreviewDevices.PHONE_HEIGHT,
    showBackground = true,
    uiMode = NIGHT
)
annotation class PanePreviews

private const val SCREEN = "Экран"
private const val PANE = "Панель"
private const val ELEMENT = "Элемент"

/** Тёмная тема системы: по ней тема кассы с выбором «как в системе» становится тёмной. */
private const val NIGHT = AndroidUiModes.UI_MODE_NIGHT_YES or AndroidUiModes.UI_MODE_TYPE_NORMAL
