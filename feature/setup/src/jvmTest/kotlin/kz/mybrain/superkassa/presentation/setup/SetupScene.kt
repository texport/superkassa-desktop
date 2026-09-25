package kz.mybrain.superkassa.presentation.setup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import io.github.texport.superkassa.core.presentation.api.model.reference.OfdEnvironmentResponse
import io.github.texport.superkassa.core.presentation.api.model.reference.TrilingualMessageResponse
import kz.mybrain.superkassa.CabinetStepsRig
import kz.mybrain.superkassa.Windowed
import kz.mybrain.superkassa.domain.setup.model.CabinetRecord
import kz.mybrain.superkassa.domain.setup.model.SetupStep
import kz.mybrain.superkassa.domain.setup.port.FakeSetupCabinet
import kz.mybrain.superkassa.domain.setup.port.SetupPorts
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.FakeCore
import kz.mybrain.superkassa.kassa.MemorySetup
import kz.mybrain.superkassa.kassa.services
import kz.mybrain.superkassa.presentation.common.cabinet.CabinetSteps
import kz.mybrain.superkassa.presentation.setup.registration.RegistrationViewModel
import kz.mybrain.superkassa.presentation.setup.registration.registrationModel
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Мастер подключения поверх кассы процесса для снимков и нажатий.
 *
 * Касса называет контуры и кассы рабочего места; пройденное — в памяти
 * проверки, а не на диске машины.
 *
 * @param contours контуры со слов кассы; `null` — касса их не называет.
 */
internal class SetupScene(
    contours: List<String>? = listOf("DEV", "TEST", "PROD"),
    kkms: List<KkmResponse> = emptyList()
) {
    val core = FakeCore().apply {
        if (contours == null) {
            refuse("getOfdEnvironments", "INTERNAL")
        } else {
            on("getOfdEnvironments") { contours.map(::contour) }
        }
        on("listKkms") { CoreScene.page(kkms) }
    }
    val memory = MemorySetup()
    val cabinet = FakeSetupCabinet()
    val calls = DirectCalls()
    val texts = textsOf(Language.Ru).setup

    /** Пройденное до открытия мастера: номер и, если [halfway], касса в кабинете. */
    fun started(halfway: Boolean = false): SetupScene = apply {
        memory.setupValue("factory", FACTORY)
        memory.setupValue("year", YEAR)
        if (halfway) {
            memory.setupValue("register", "r-1")
            memory.setupValue("system", "5000021")
            memory.setupValue("name", "Касса у входа")
        }
    }

    /** Касса в кабинете уже на учёте: номер КГД выдан, и кабинет выпустит ей токен. */
    fun registered(): SetupScene = apply { cabinet.record = CabinetRecord("REGISTERED", KGD_NUMBER) }

    /** Модель мастера, как её создаст окно; создаётся при подменённом главном потоке. */
    fun model(): SetupViewModel = setupModel(CoreScene.services(core), SetupPorts(memory, cabinet), calls)

    /** Модель шага постановки на учёт поверх того же кабинета. */
    fun registration(): RegistrationViewModel = registrationModel(SetupPorts(memory, cabinet), calls)

    private fun contour(code: String) = OfdEnvironmentResponse(code, TrilingualMessageResponse(code, code, code))

    companion object {
        const val FACTORY = "KZT26E2C509A200"
        const val YEAR = "2026"
        const val KGD_NUMBER = "010100253145"
        const val NO_PLACES = """{"page":0,"size":50,"totalElements":0,"items":[]}"""
    }
}

/**
 * Последний шаг пути через кабинет, как его открывает «Далее»: владелец
 * вошёл в кабинет, касса в кабинете прочитана.
 */
@Composable
internal fun AdminStepAlone(scene: SetupScene, model: SetupViewModel) {
    val models = remember { SetupModels(model, scene.registration()) }
    val cabinet = remember { CabinetStepsRig.signedIn(SetupScene.NO_PLACES) }
    Windowed { ConnectKkmScreen(models, cabinet, step = SetupStep.Admin.name) }
}

/** Мастер в истории [history] поверх сцены [scene]; вход в кабинет — [cabinet]. */
@Composable
internal fun WizardOf(scene: SetupScene, history: WizardHistory, cabinet: CabinetSteps, onBack: (() -> Unit)? = null) {
    val models = remember { SetupModels(scene.model(), scene.registration()) }
    Windowed { WizardShown(history) { step -> ConnectKkmScreen(models, cabinet, step, onBack) } }
}
