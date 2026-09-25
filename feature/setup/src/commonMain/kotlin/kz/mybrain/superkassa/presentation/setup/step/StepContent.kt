package kz.mybrain.superkassa.presentation.setup.step

import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.domain.setup.model.SetupStep
import kz.mybrain.superkassa.presentation.setup.SetupParts

/**
 * Что делается на шаге [step] — и только это.
 *
 * Шаги кабинета и учёта бывают только на пути через кабинет, и кабинет
 * у них есть всегда; без него их нет на пути вовсе.
 */
@Composable
internal fun StepContent(step: SetupStep, parts: SetupParts) {
    val office = parts.office
    when (step) {
        SetupStep.Way -> WayStep(parts.state, parts.actions)
        SetupStep.Factory -> FactoryStep(parts.state, parts.actions)
        SetupStep.Cabinet -> office?.let { CabinetStep(it, parts.state.draft, parts.actions) }
        SetupStep.Application -> office?.let { ApplicationStep(it, parts.state.draft.cabinetRegisterId) }
        SetupStep.Credentials -> CredentialsStep(parts.state, parts.actions)
        SetupStep.Admin -> AdminStep(parts)
    }
}
