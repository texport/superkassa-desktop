package kz.mybrain.superkassa.presentation.journal.queue

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.mybrain.superkassa.presentation.shell.AppContainer

/** Модель очереди окна: одна на окно, переживает смену раздела. */
@Composable
fun queueViewModel(app: AppContainer): QueueViewModel = viewModel { queueModel(app) }

/** Модель со сценариями, собранными из портов окна; проверки зовут её без окна. */
fun queueModel(app: AppContainer): QueueViewModel = QueueViewModel(QueueCases(app.kassa, app.signIn), app.talk)
