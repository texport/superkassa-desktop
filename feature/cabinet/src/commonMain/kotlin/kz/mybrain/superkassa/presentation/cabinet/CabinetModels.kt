package kz.mybrain.superkassa.presentation.cabinet

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.mybrain.superkassa.domain.cabinet.port.CabinetPorts
import kz.mybrain.superkassa.presentation.common.model.WindowServices

/**
 * Модель кабинета окна.
 *
 * Одна на окно: живёт в хранилище моделей окна и переживает уход владельца
 * на вход кассы и обратно.
 */
@Composable
fun cabinetViewModel(services: WindowServices, ports: CabinetPorts): CabinetViewModel =
    viewModel { cabinetModel(services, ports) }

/** Модель кабинета над портами кабинета и кассой процесса — и в окне, и в проверке. */
fun cabinetModel(services: WindowServices, ports: CabinetPorts): CabinetViewModel =
    CabinetViewModel(cabinetCases(services, ports), services.talk)

/** Сценарии кабинета над портами точки сборки. */
internal fun cabinetCases(services: WindowServices, ports: CabinetPorts): CabinetCases =
    CabinetCases(services.kassa, services.signIn, services.memory, ports)
