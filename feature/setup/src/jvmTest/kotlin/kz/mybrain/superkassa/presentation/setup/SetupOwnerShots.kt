package kz.mybrain.superkassa.presentation.setup

import kz.mybrain.superkassa.CabinetStepsRig
import kz.mybrain.superkassa.OwnerShots
import kz.mybrain.superkassa.Windowed
import kz.mybrain.superkassa.kassa.inlineMain
import kotlin.test.Test

/** Мастер новой кассы по замечаниям владельца — в окне ноутбука и на мониторе, `/tmp/owner-setup-<окно>.png`. */
class SetupOwnerShots {

    @Test
    fun `мастер новой кассы`() = OwnerShots.each { width, height ->
        val scene = inlineMain { SetupScene().started() }
        val models = inlineMain { SetupModels(scene.model(), scene.registration()) }
        val cabinet = CabinetStepsRig.signedIn(SetupScene.NO_PLACES)
        OwnerShots.save("setup", width, height) { Windowed { ConnectKkmScreen(models, cabinet) {} } }
    }
}
