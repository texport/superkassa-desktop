package kz.mybrain.superkassa.presentation.analytics.sales

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.mybrain.superkassa.presentation.analytics.AnalyticsPorts
import kz.mybrain.superkassa.presentation.common.model.WindowModels

/**
 * Модель сводки сети: одна на окно.
 *
 * Живёт в хранилище моделей окна и переживает уход владельца в другой
 * раздел — вернувшись, он видит прочитанное, а не ждёт его заново.
 */
@Composable
fun analyticsSalesViewModel(ports: AnalyticsPorts): AnalyticsSalesViewModel = viewModel { analyticsSalesModel(ports) }

/**
 * Сводка одной кассы — модель живёт, пока открыто её окно.
 *
 * Хранилище у окна своё: закрытое окно останавливает и перечитывание
 * сводки, а открытое заново начинает с чистого листа.
 */
@Composable
fun kkmSalesViewModel(ports: AnalyticsPorts, register: String): AnalyticsSalesViewModel {
    val models = remember(register) { WindowModels() }
    DisposableEffect(models) { onDispose { models.close() } }
    return viewModel(viewModelStoreOwner = models, key = register) { analyticsSalesModel(ports, register) }
}

/**
 * Модель сводки без окна — для проверок и для окна.
 *
 * @param register касса отбора; `null` — вся сеть.
 */
fun analyticsSalesModel(ports: AnalyticsPorts, register: String? = null): AnalyticsSalesViewModel =
    AnalyticsSalesViewModel(SalesCases(ports.cabinet), register)
