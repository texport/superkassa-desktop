package kz.mybrain.superkassa.presentation.setup

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.mybrain.superkassa.domain.setup.port.SetupPorts
import kz.mybrain.superkassa.presentation.common.cabinet.CabinetCalls
import kz.mybrain.superkassa.presentation.common.model.WindowServices

/**
 * Модель мастера подключения окна.
 *
 * Одна на окно: набранное переживает уход с мастера, а пройденное —
 * и закрытие приложения, через память пройденного.
 *
 * @param calls обращения к кабинету окна: занятость и слова помех — его.
 */
@Composable
fun setupViewModel(services: WindowServices, ports: SetupPorts, calls: CabinetCalls): SetupViewModel =
    viewModel { setupModel(services, ports, calls) }

/** Модель со сценариями, собранными из портов окна; проверки зовут её без окна. */
fun setupModel(services: WindowServices, ports: SetupPorts, calls: CabinetCalls): SetupViewModel =
    SetupViewModel(SetupCases(services.kassa, ports, services.talk.journal), services.talk, calls)
