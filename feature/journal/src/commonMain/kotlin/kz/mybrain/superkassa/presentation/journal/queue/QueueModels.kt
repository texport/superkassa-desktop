package kz.mybrain.superkassa.presentation.journal.queue

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.mybrain.superkassa.presentation.common.model.WindowServices

/** Модель очереди окна: одна на окно, переживает смену раздела. */
@Composable
fun queueViewModel(services: WindowServices): QueueViewModel = viewModel { queueModel(services) }

/** Модель со сценариями, собранными из портов окна; проверки зовут её без окна. */
internal fun queueModel(services: WindowServices): QueueViewModel =
    QueueViewModel(QueueCases(services.kassa, services.signIn), services.talk)
