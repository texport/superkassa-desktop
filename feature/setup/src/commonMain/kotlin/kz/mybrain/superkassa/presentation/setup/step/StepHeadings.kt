package kz.mybrain.superkassa.presentation.setup.step

import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.designsystem.wizard.WizardHeading
import kz.mybrain.superkassa.domain.setup.model.SetupStep
import kz.mybrain.superkassa.domain.setup.model.SetupWay
import kz.mybrain.superkassa.strings.api.setup.SetupTexts

/**
 * Иллюстрация, название и объяснение шага [step].
 *
 * Заводской номер и пин на двух путях объясняются по-разному: на ручном
 * номер нужен тому, кто ещё будет заводить кассу, а токен владелец набрал
 * сам — о выпуске токена кабинетом там ни слова.
 */
internal fun headingOf(step: SetupStep, way: SetupWay, texts: SetupTexts): WizardHeading {
    val byHand = way == SetupWay.ByHand
    return when (step) {
        SetupStep.Way -> WizardHeading(AppIcons.setupWay, texts.stepWay, texts.wayExplain)
        SetupStep.Factory -> WizardHeading(
            AppIcons.setupFactory,
            texts.stepFactory,
            if (byHand) texts.factoryExplainManual else texts.factoryExplain
        )
        SetupStep.Cabinet -> WizardHeading(AppIcons.setupCabinet, texts.stepCabinet, texts.cabinetExplain)
        SetupStep.Application ->
            WizardHeading(AppIcons.setupApplication, texts.stepApplication, texts.applicationExplain)
        SetupStep.Credentials ->
            WizardHeading(AppIcons.setupCredentials, texts.stepCredentials, texts.credentialsExplain)
        SetupStep.Admin -> WizardHeading(
            AppIcons.setupAdmin,
            texts.stepAdmin,
            if (byHand) texts.adminExplainManual else texts.adminExplain
        )
    }
}
