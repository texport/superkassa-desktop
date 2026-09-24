package kz.mybrain.superkassa.presentation.journal.documents

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.mybrain.superkassa.domain.journal.port.JournalPorts
import kz.mybrain.superkassa.presentation.common.model.WindowServices

/**
 * Модель журнала окна.
 *
 * Одна на окно: срок, отбор и прочитанное переживают уход кассира
 * в продажу и обратно.
 */
@Composable
fun journalViewModel(services: WindowServices, ports: JournalPorts): JournalViewModel =
    viewModel { journalModel(services, ports) }

/** Модель со сценариями, собранными из портов окна; проверки зовут её без окна. */
internal fun journalModel(services: WindowServices, ports: JournalPorts): JournalViewModel =
    JournalViewModel(JournalCases(services.kassa, services.signIn, ports), services.talk)
