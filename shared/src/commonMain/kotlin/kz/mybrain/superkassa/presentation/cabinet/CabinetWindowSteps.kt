package kz.mybrain.superkassa.presentation.cabinet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.presentation.cabinet.applications.ApplicationSignWait
import kz.mybrain.superkassa.presentation.cabinet.enroll.AddRegisterDialog
import kz.mybrain.superkassa.presentation.cabinet.enroll.FactoryStamp
import kz.mybrain.superkassa.presentation.cabinet.signin.SignInAction
import kz.mybrain.superkassa.presentation.common.cabinet.CabinetCalls
import kz.mybrain.superkassa.presentation.common.cabinet.CabinetSession
import kz.mybrain.superkassa.presentation.common.cabinet.CabinetSteps
import kz.mybrain.superkassa.presentation.common.model.collectAsScreenState
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Шаги этого кабинета окна для мастера подключения.
 *
 * Мастер видит только [CabinetSteps]: вход, форма кассы и ожидание
 * подписи остаются здесь и в мастере те же, что в разделах кабинета.
 */
fun CabinetWindow.steps(): CabinetSteps = WindowSteps(this)

/** Шаги кабинета поверх его модели и формы кассы из раздела точек. */
private class WindowSteps(private val window: CabinetWindow) : CabinetSteps {
    private val model = window.cabinet

    /** Обращения мастера идут работой кабинета окна: его занятость и его слова о помехах. */
    override val calls: CabinetCalls = object : CabinetCalls {
        override suspend fun <T> run(action: String, block: suspend () -> T): T? = model.work.run(action, block).value
    }

    @Composable
    override fun session(): CabinetSession {
        val state by model.state.collectAsScreenState()
        return CabinetSession(open = state.open, busy = state.busy)
    }

    @Composable
    override fun SignIn() {
        val language = LocalLanguage.current
        SignInAction(cabinet = model, language = language, texts = textsOf(language).cabinet, modifier = Modifier)
    }

    @Composable
    override fun AddRegister(
        factoryNumber: String,
        year: String,
        onDismiss: () -> Unit,
        onAdded: (CabinetRegister) -> Unit
    ) {
        val texts = textsOf(LocalLanguage.current).cabinet
        AddRegisterDialog(window, texts, FactoryStamp(factoryNumber, year), onDismiss = onDismiss, onAdded = onAdded)
    }

    @Composable
    override fun SignWait(onCancel: () -> Unit) {
        val language = LocalLanguage.current
        ApplicationSignWait(language, textsOf(language).cabinet, onCancel)
    }
}
