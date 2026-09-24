package kz.mybrain.superkassa.presentation.cabinet

import kz.mybrain.superkassa.presentation.shell.AppContainer
import kz.mybrain.superkassa.presentation.theme.choice.LookViewModel

/**
 * Кабинет окна и то, что разделам кабинета нужно от окна.
 *
 * Разделы кабинета берут отсюда модель кабинета — вошедшего, списки,
 * занятость, — вид окна (свёрнута ли колонка точек) и контейнер окна:
 * карта выбора места берёт из него свои службы, заведение кассы — вход.
 * По отдельности всё это протягивалось бы тремя параметрами через
 * каждый слой разметки.
 */
class CabinetWindow(val app: AppContainer, val cabinet: CabinetViewModel, val look: LookViewModel)
