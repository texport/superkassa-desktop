package kz.mybrain.superkassa.presentation.setup

import kz.mybrain.superkassa.CabinetStepsRig
import kz.mybrain.superkassa.OwnerShots
import kz.mybrain.superkassa.domain.setup.model.SetupStep
import kz.mybrain.superkassa.kassa.inlineMain
import kotlin.test.Test

/**
 * Мастер новой кассы по замечаниям владельца — каждый шаг пути через кабинет
 * на телефоне, планшете стоймя, ноутбуке и мониторе,
 * `/tmp/owner-setup-<шаг>-<окно>.png`.
 */
class SetupOwnerShots {

    @Test
    fun `шаги мастера новой кассы`() = SIZES.forEach { (width, height) ->
        SHOWN.forEach { (name, steps) ->
            val scene = inlineMain { SetupScene().started(halfway = steps.size > 2) }
            val history = WizardHistory(*steps.toTypedArray())
            val cabinet = CabinetStepsRig.signedIn(SetupScene.NO_PLACES)
            OwnerShots.save("setup-$name", width, height) { WizardOf(scene, history, cabinet) }
        }
    }

    private companion object {
        /** Телефон, планшет стоймя, ноутбук и монитор. */
        val SIZES = listOf(412 to 915, 800 to 1280, 1280 to 800, 1920 to 1080)

        /** Шаг и путь к нему: первый — сам раздел, остальные — поверх него. */
        val SHOWN: List<Pair<String, List<String>>> = listOf(
            SetupStep.Way,
            SetupStep.Factory,
            SetupStep.Cabinet,
            SetupStep.Application,
            SetupStep.Admin
        ).let { route -> route.mapIndexed { at, step -> step.name to route.drop(1).take(at).map { it.name } } }
    }
}
