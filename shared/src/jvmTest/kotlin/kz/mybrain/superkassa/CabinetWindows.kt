package kz.mybrain.superkassa

import kz.mybrain.superkassa.kassa.areaPorts
import kz.mybrain.superkassa.presentation.cabinet.CabinetWindow
import kz.mybrain.superkassa.presentation.common.look.LookViewModel
import kz.mybrain.superkassa.presentation.shell.AppContainer
import kz.mybrain.superkassa.presentation.shell.frame.cabinetNeighbours

/*
 * Кабинет в окне каркаса: те же сцены кабинета, что у его проверок, но
 * с соседями, которых кабинету подставляет каркас окна.
 */

/** Кабинет окна [app], в который никто не входил: окно кассы без владельца. */
internal fun windowCabinet(app: AppContainer, look: LookViewModel): CabinetWindow =
    idleCabinet(app.services, look, cabinetNeighbours(app))

/** Контейнер окна над сценой кабинета: общие службы сцены и порты областей проверки. */
internal fun CabinetStage.app(): AppContainer = AppContainer(services, areaPorts())
