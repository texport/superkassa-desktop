package kz.mybrain.superkassa.presentation.journal.documents

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.mybrain.superkassa.presentation.shell.AppContainer

/**
 * Модель журнала окна.
 *
 * Одна на окно: срок, отбор и прочитанное переживают уход кассира
 * в продажу и обратно.
 */
@Composable
fun journalViewModel(app: AppContainer): JournalViewModel = viewModel { journalModel(app) }

/** Модель со сценариями, собранными из портов окна; проверки зовут её без окна. */
fun journalModel(app: AppContainer): JournalViewModel =
    JournalViewModel(JournalCases(app.kassa, app.signIn, app.areas.journal), app.talk)
