package kz.mybrain.superkassa.presentation.setup.registration

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.mybrain.superkassa.domain.setup.port.SetupPorts
import kz.mybrain.superkassa.presentation.setup.CabinetCalls

/** Модель шага постановки на учёт окна: поданное и прочитанное переживают уход с мастера. */
@Composable
fun registrationViewModel(ports: SetupPorts, calls: CabinetCalls): RegistrationViewModel =
    viewModel { registrationModel(ports, calls) }

/** Модель со сценариями, собранными из портов мастера; проверки зовут её без окна. */
fun registrationModel(ports: SetupPorts, calls: CabinetCalls): RegistrationViewModel =
    RegistrationViewModel(RegistrationCases(requireNotNull(ports.cabinet) { NO_CABINET }), calls)

/** Постановку на учёт подают через кабинет, а кабинета у точки сборки этой платформы нет. */
private const val NO_CABINET = "registration is filed through the cabinet, which is not assembled on this platform"
