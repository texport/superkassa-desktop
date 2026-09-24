package kz.mybrain.superkassa.presentation.common.look

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.mybrain.superkassa.domain.workplace.model.WorkplaceLook

/**
 * Модель вида окна.
 *
 * Одна на окно: живёт в хранилище моделей окна, и тема, шапка, настройки
 * и кабинет берут один и тот же экземпляр.
 *
 * @param look вид рабочего места: язык, тема, шрифт и свёрнутые части.
 */
@Composable
fun lookViewModel(look: WorkplaceLook): LookViewModel = viewModel { lookModel(look) }

/** Модель над видом рабочего места; проверки зовут её без окна. */
fun lookModel(look: WorkplaceLook): LookViewModel = LookViewModel(LookCases(look))
