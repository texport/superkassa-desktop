package kz.mybrain.superkassa.kassa

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kz.mybrain.superkassa.presentation.AppContainer
import kz.mybrain.superkassa.presentation.DoorShell
import kz.mybrain.superkassa.presentation.ProvideWindowModels
import kz.mybrain.superkassa.presentation.WindowModels
import kz.mybrain.superkassa.presentation.session.CabinetSession
import kz.mybrain.superkassa.presentation.session.Session

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
    fun Door(session: Session, app: AppContainer) {
        val models = remember { WindowModels() }
        ProvideWindowModels(models) {
            DoorShell(session, app, remember { CabinetSession() }, remember { SnackbarHostState() })
        }
    }
}

/**
 * Выполняет [block] с главным потоком, который исполняет работу сразу.
 *
 * Модели экранов запускают работу в главном потоке окна; в проверке окна
 * нет, и без подмены ответ кассы доезжал бы до кадра когда придётся.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun <T> inlineMain(block: () -> T): T {
    Dispatchers.setMain(Dispatchers.Unconfined)
    return try {
        block()
    } finally {
        Dispatchers.resetMain()
    }
}
