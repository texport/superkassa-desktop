package kz.mybrain.superkassa.presentation.setup

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.mybrain.superkassa.domain.setup.port.SetupPorts
import kz.mybrain.superkassa.presentation.shell.AppContainer

/**
 * Модель мастера подключения окна.
 *
 * Одна на окно: набранное переживает уход с мастера, а пройденное —
 * и закрытие приложения, через память пройденного.
 *
 * @param calls обращения к кабинету окна: занятость и слова помех — его.
 */
@Composable
fun setupViewModel(app: AppContainer, ports: SetupPorts, calls: CabinetCalls): SetupViewModel =
    viewModel { setupModel(app, ports, calls) }

/** Модель со сценариями, собранными из портов окна; проверки зовут её без окна. */
fun setupModel(app: AppContainer, ports: SetupPorts, calls: CabinetCalls): SetupViewModel =
    SetupViewModel(SetupCases(app.kassa, ports, app.journal), app.talk, calls)
