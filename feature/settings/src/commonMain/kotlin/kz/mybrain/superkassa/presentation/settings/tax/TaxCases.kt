package kz.mybrain.superkassa.presentation.settings.tax

import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.settings.usecase.tax.ReadTaxDictionaries
import kz.mybrain.superkassa.domain.settings.usecase.tax.SaveKkmSwitches
import kz.mybrain.superkassa.domain.settings.usecase.tax.SaveTaxSettings
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.domain.signin.usecase.ObserveSignIn

/** Сценарии налогов кассы. */
class TaxCases(kassa: Kassa, signIn: SignIn) {
    val observe = ObserveSignIn(signIn)
    val dictionaries = ReadTaxDictionaries(kassa)
    val save = SaveTaxSettings(kassa, signIn)
    val switches = SaveKkmSwitches(kassa, signIn)
}
