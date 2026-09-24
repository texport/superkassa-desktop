package kz.mybrain.superkassa.kassa

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import kz.mybrain.superkassa.KassaDesk
import kz.mybrain.superkassa.presentation.common.model.ProvideWindowModels
import kz.mybrain.superkassa.presentation.common.model.WindowModels
import kz.mybrain.superkassa.presentation.shell.AppContainer
import kz.mybrain.superkassa.presentation.shell.frame.DoorShell

/**
 * Вход в том самом окне, каким его собирает приложение, — поверх кассы процесса.
 *
 * Модель входа живёт в хранилище моделей окна, как в приложении; касса
 * отвечает тем, что дала проверка.
 */
object LoginScene {

    /** Касса, у которой заведены [kkms]; `null` — список она не отдаёт. */
    fun core(kkms: List<KkmResponse>?): FakeCore = FakeCore().apply {
        if (kkms != null) on("listKkms") { CoreScene.page(kkms) }
    }

    @Composable
    fun Door(app: AppContainer) {
        val models = remember { WindowModels() }
        ProvideWindowModels(models) {
            DoorShell(app, remember { KassaDesk(app).parts }, remember { SnackbarHostState() })
        }
    }
}
