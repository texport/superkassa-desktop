package kz.mybrain.superkassa.presentation.cabinet

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.presentation.shell.AppContainer

/**
 * Кабинет окна и то, что разделам кабинета нужно от окна.
 *
 * Разделы кабинета берут отсюда модель кабинета — вошедшего, списки,
 * занятость, — вид окна (свёрнута ли колонка точек) и контейнер окна:
 * карта выбора места берёт из него свои службы, заведение кассы — вход.
 * По отдельности всё это протягивалось бы тремя параметрами через
 * каждый слой разметки.
 */
class CabinetWindow(val app: AppContainer, val cabinet: CabinetViewModel, val look: CabinetLook)

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
