package kz.mybrain.superkassa.presentation.cabinet

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.mybrain.superkassa.presentation.shell.AppContainer

/**
 * Модель кабинета окна.
 *
 * Одна на окно: живёт в хранилище моделей окна и переживает уход владельца
 * на вход кассы и обратно.
 */
@Composable
fun cabinetViewModel(app: AppContainer): CabinetViewModel = viewModel { cabinetModel(app) }

/** Модель кабинета над портами кабинета и кассой процесса — и в окне, и в проверке. */
fun cabinetModel(app: AppContainer): CabinetViewModel = CabinetViewModel(cabinetCases(app), app.talk)

/** Сценарии кабинета над портами точки сборки. */
fun cabinetCases(app: AppContainer): CabinetCases =
    CabinetCases(app.kassa, app.signIn, app.memory, requireNotNull(app.areas.cabinet) { NO_CABINET })

/** Кабинета нет у точки сборки этой платформы, а раздел открыли. */
private const val NO_CABINET = "cabinet ports are not assembled on this platform"
