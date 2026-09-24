package kz.mybrain.superkassa

import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.domain.workplace.port.WorkplaceMemory
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.FakeCore
import kz.mybrain.superkassa.kassa.MemoryWorkplace
import kz.mybrain.superkassa.kassa.app
import kz.mybrain.superkassa.presentation.common.look.lookModel
import kz.mybrain.superkassa.presentation.shell.AppContainer
import kz.mybrain.superkassa.presentation.shell.frame.WindowParts
import kz.mybrain.superkassa.presentation.shell.frame.shellModel

/**
 * Рабочее окно с кассой [kkm], за которой вошёл кассир; `null` — никто не вошёл.
 *
 * @param admin кассир — администратор: ему видны очередь, кассиры и настройки.
 */
internal fun KassaScene.desk(
    kkm: KkmResponse? = kkm(),
    admin: Boolean = true,
    core: FakeCore = FakeCore(),
    memory: WorkplaceMemory = MemoryWorkplace()
): KassaDesk {
    val signIn = SignIn()
    kkm?.let { signIn.enter(it, CoreScene.cashier(admin), PIN) }
    return KassaDesk(CoreScene.app(core, signIn, memory = memory))
}

/**
 * Окно кассы для снимка: контейнер окна и модели каркаса — как в приложении.
 *
 * Кабинет у окна есть, но в него никто не входил: окно кассы без владельца.
 */
internal class KassaDesk(val app: AppContainer) {
    val look = lookModel(app.services.look)
    val parts = WindowParts(shellModel(app), look, idleCabinet(app, look))
}
