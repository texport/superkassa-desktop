package kz.mybrain.superkassa.presentation.cabinet

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.domain.cabinet.model.RegisterAddress
import kz.mybrain.superkassa.domain.setup.port.SetupMemory
import kz.mybrain.superkassa.presentation.common.mapview.MapPoint
import kz.mybrain.superkassa.presentation.common.mapview.MapRegistry
import kz.mybrain.superkassa.presentation.common.mapview.PointPicker
import kz.mybrain.superkassa.strings.api.cabinet.CabinetTexts

/**
 * Кабинет окна и то, что разделам кабинета нужно от окна.
 *
 * Разделы кабинета берут отсюда модель кабинета — вошедшего, списки,
 * занятость, — вид окна (свёрнута ли колонка точек) и то, что дают
 * соседние области. По отдельности всё это протягивалось бы тремя
 * параметрами через каждый слой разметки.
 */
class CabinetWindow(
    val cabinet: CabinetViewModel,
    val look: CabinetLook,
    val neighbours: CabinetNeighbours = CabinetNeighbours()
)

/**
 * Что кабинету нужно от вида окна: свёрнута ли колонка точек и
 * переключатели темы и языка в шапке.
 *
 * Вид окна выбирают в настройках, и модель его живёт там. Кабинет её
 * не знает: каркас окна, который видит все области, отдаёт ему только
 * эти три вещи, и раздел не зависит от чужой области.
 *
 * @property placesCollapsed свёрнута ли колонка точек — читается в разметке.
 * @property switches тема и язык в шапке кабинета — те же, что у кассы.
 */
class CabinetLook(
    val placesCollapsed: @Composable () -> Boolean,
    val togglePlaces: () -> Unit,
    val switches: @Composable RowScope.() -> Unit
)

/**
 * Что кабинету дают соседние области — их подставляет каркас окна.
 *
 * Кабинет о соседях не знает: вкладка аналитики, окно карты, память
 * мастера и список касс входа приходят готовыми. Без них — в снимках
 * одного кабинета — вкладка пуста, а окно карты не открывается.
 *
 * @property analytics вкладка аналитики: отметка вошедшего и надписи кабинета.
 * @property points окно выбора места торговой точки на карте.
 * @property setupMemory память мастера подключения: касса, заведённая здесь,
 *   запоминает контур так же, как мастер; `null` — мастера на платформе нет.
 * @property kkmsReload перечитать кассы входа: новая касса появляется там
 *   сразу. Берётся в разметке — там, где у окна уже есть модель входа.
 */
class CabinetNeighbours(
    val analytics: @Composable (access: String?, texts: CabinetTexts) -> Unit = { _, _ -> },
    val points: PointPicker = NoPoints,
    val setupMemory: SetupMemory? = null,
    val kkmsReload: @Composable () -> () -> Unit = { {} }
)

/** Окна карты нет: форма точки остаётся без выбора на карте. */
private object NoPoints : PointPicker {
    @Composable
    override fun Show(
        registry: MapRegistry,
        point: MapPoint?,
        address: RegisterAddress?,
        onAddress: (RegisterAddress) -> Unit,
        onDismiss: () -> Unit,
        onPicked: (MapPoint) -> Unit
    ) = Unit
}
