package kz.mybrain.superkassa.presentation.common.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner

/**
 * Модели экранов одного окна.
 *
 * Живут, пока открыто окно, и переживают смену раздела: корзина продажи
 * и набранное на входе не теряются, когда кассир отлучился в журнал.
 * На Android хранилище моделей даёт сама активность, на настольной кассе —
 * этот объект, созданный вместе с окном.
 */
class WindowModels : ViewModelStoreOwner {
    override val viewModelStore: ViewModelStore = ViewModelStore()

    /** Окно закрыто: модели останавливают свою работу. */
    fun close() = viewModelStore.clear()
}

/** Отдаёт модели окна фабрикам моделей областей. */
@Composable
fun ProvideWindowModels(models: WindowModels, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalViewModelStoreOwner provides models, content = content)
}
