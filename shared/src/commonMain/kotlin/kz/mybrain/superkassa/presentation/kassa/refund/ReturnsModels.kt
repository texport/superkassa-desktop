package kz.mybrain.superkassa.presentation.kassa.refund

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.mybrain.superkassa.domain.kassa.port.KassaPorts
import kz.mybrain.superkassa.presentation.common.model.WindowServices

/**
 * Модель возврата окна.
 *
 * Одна на окно: выбранный чек и набранная сумма переживают уход кассира
 * в другой раздел.
 */
@Composable
fun returnsViewModel(services: WindowServices, ports: KassaPorts): ReturnsViewModel =
    viewModel { returnsModel(services, ports) }

/** Действия экрана, выполняемые этой моделью. */
fun ReturnsViewModel.actions(): ReturnsActions = ReturnsActions(this, refund, payments)

/** Модель со сценариями, собранными из портов окна; проверки зовут её без окна. */
fun returnsModel(services: WindowServices, ports: KassaPorts): ReturnsViewModel =
    ReturnsViewModel(RefundCases(services.kassa, services.signIn, services.memory, ports), services.talk)
